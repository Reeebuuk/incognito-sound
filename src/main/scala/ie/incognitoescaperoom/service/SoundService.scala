package ie.incognitoescaperoom.service

import ie.incognitoescaperoom.settings.{Settings, SoundSettings}
import ie.incognitoescaperoom.settings.{Settings, SoundSettings}
import zio.*

import java.math.MathContext
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID
import javax.sound.sampled.*

trait SoundServiceApi:
  def getSound(filename: String): Option[SoundStreamData]

final case class SoundService(settings: Settings) extends SoundServiceApi:

  private val mixerInfo = AudioSystem.getMixerInfo

  private val allSounds: List[SoundStreamData] = settings.sounds.map(getStreamData)

  override def getSound(filename: String): Option[SoundStreamData] =
    allSounds.find(_.filename == filename)

  private def getStreamData(soundSettings: SoundSettings): SoundStreamData =
    val resource    = ClassLoader.getSystemClassLoader.getResource(soundSettings.filename)
    val mixer       = AudioSystem.getMixer(mixerInfo.find(_.getName == soundSettings.deviceName).get)
    val audioStream = AudioSystem.getAudioInputStream(resource)
    val format      = audioStream.getFormat
    val info        = new DataLine.Info(classOf[SourceDataLine], format)
    val line        = mixer.getLine(info).asInstanceOf[SourceDataLine]
    line.open(format)
    SoundStreamData(soundSettings.deviceName, soundSettings.filename, soundSettings.isBackgroundMusic, audioStream, line, format)

case class SoundStreamData(
  deviceName:        String,
  filename:          String,
  isBackgroundMusic: Boolean,
  audioStream:       AudioInputStream,
  line:              SourceDataLine,
  format:            AudioFormat
):

  private val volumeControl = line.getControl(FloatControl.Type.MASTER_GAIN).asInstanceOf[FloatControl]

  @volatile
  var paused: Boolean = false
  @volatile
  var currentPosition: Long = 0

  def setVolume(value: Float): Unit =
    volumeControl.setValue(value)

  def stop(): Unit =
    jumpTo(0)
    line.stop()
    paused = true

  def pause(): Unit =
    line.stop()
    paused = true

  def resume(): Unit =
    line.start()
    paused = false

  def close(): Unit =
    line.drain()
    line.stop()
    line.close()
    audioStream.close()

  def jumpTo(seconds: Long): Unit = {
    val bytesPerSecond     = format.getFrameSize * format.getFrameRate
    val targetBytePosition = seconds * bytesPerSecond
    val buffer             = new Array[Byte](targetBytePosition.toInt)
    audioStream.read(buffer, 0, buffer.length)
    line.flush()
    line.write(buffer, 0, buffer.length)
    currentPosition = targetBytePosition.toLong
  }

  def play(): Unit =
    line.start()

    val buffer = new Array[Byte](1024 * 4)
    var n = 0
    while (n != -1) {
      n = audioStream.read(buffer, 0, buffer.length)
      if (n > 0) {
        line.write(buffer, 0, n)
      }
      if (paused) {
        currentPosition += n
      }
    }

object SoundService:
  val live = ZLayer.fromFunction(SoundService.apply _)
