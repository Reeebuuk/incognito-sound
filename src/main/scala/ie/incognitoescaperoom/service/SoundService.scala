package ie.incognitoescaperoom.service

import ie.incognitoescaperoom.settings.{ Settings, SoundSettings }
import zio.*

import java.math.MathContext
import java.time.{ Instant, LocalDateTime, ZoneId }
import java.util.UUID
import java.util.concurrent.Semaphore
import javax.sound.sampled.*
import scala.annotation.tailrec

trait SoundServiceApi:
  def getSound(filename:        String): Option[SoundStreamDataHandler]
  def setPlayingFiber(filename: String, fiber: Fiber.Runtime[Throwable, Unit]): Unit

final case class SoundService(settings: Settings) extends SoundServiceApi:

  private val mixerInfo: Array[Mixer.Info] = AudioSystem.getMixerInfo

  private val allSounds: List[SoundStreamDataHandler] = settings.sounds.map(SoundStreamDataHandler(_, mixerInfo))

  override def setPlayingFiber(filename: String, fiber: Fiber.Runtime[Throwable, Unit]): Unit =
    allSounds.find(_.soundSettings.filename == filename).foreach(_.setPlayFiber(fiber))

  override def getSound(filename: String): Option[SoundStreamDataHandler] =
    allSounds.find(_.soundSettings.filename == filename)

case class SoundStreamData(
  deviceName:        String,
  filename:          String,
  isBackgroundMusic: Boolean,
  audioStream:       AudioInputStream,
  line:              SourceDataLine,
  format:            AudioFormat
)

object SoundStreamData:
  def apply(soundSettings: SoundSettings, mixerInfo: Array[Mixer.Info]): SoundStreamData =
    val resource    = ClassLoader.getSystemClassLoader.getResource(soundSettings.filename)
    val mixer       = AudioSystem.getMixer(mixerInfo.find(_.getName == soundSettings.deviceName).get)
    val audioStream = AudioSystem.getAudioInputStream(resource)
    val format      = audioStream.getFormat
    val info        = new DataLine.Info(classOf[SourceDataLine], format)
    val line        = mixer.getLine(info).asInstanceOf[SourceDataLine]
    line.open(format)
    SoundStreamData(soundSettings.deviceName, soundSettings.filename, soundSettings.isBackgroundMusic, audioStream, line, format)

case class SoundStreamDataHandler(soundSettings: SoundSettings, mixerInfo: Array[Mixer.Info]):

  @volatile
  var soundStreamData: SoundStreamData = SoundStreamData(soundSettings, mixerInfo)

  private def volumeControl = soundStreamData.line.getControl(FloatControl.Type.MASTER_GAIN).asInstanceOf[FloatControl]

  @volatile
  var isStopped: Boolean = false
  @volatile
  var currentPosition: Long = 0
  @volatile
  var playingFiber: Option[Fiber.Runtime[Throwable, Unit]] = None

  def setVolume(value: Float): Unit =
    volumeControl.setValue(value)

  def setPlayFiber(fiber: Fiber.Runtime[Throwable, Unit]): Unit =
    playingFiber = Some(fiber)

  def stop(startFromBeginning: Boolean = false): Unit =
    if (startFromBeginning) currentPosition = 0
    soundStreamData.line.stop()
    isStopped = true
    playingFiber.map(_.interruptFork)
    playingFiber = None
    soundStreamData.line.drain()
    soundStreamData = SoundStreamData(soundSettings, mixerInfo)

  def close(): Unit =
    soundStreamData.line.drain()
    soundStreamData.line.stop()
    soundStreamData.line.close()
    soundStreamData.audioStream.close()

  def jumpTo(seconds: Long): Unit = {
    stop(true)
    val bytesPerSecond     = soundStreamData.format.getFrameSize * soundStreamData.format.getFrameRate
    val targetBytePosition = seconds * bytesPerSecond
    currentPosition = targetBytePosition.toLong
    play()
  }

  def play(): Unit =
    playInternal(soundStreamData.audioStream, soundStreamData.line)

  private def skip(seconds: Long): Unit = {
    val bytesPerSecond     = soundStreamData.format.getFrameSize * soundStreamData.format.getFrameRate
    val targetBytePosition = seconds * bytesPerSecond
    val skippedBytes       = soundStreamData.audioStream.skip(targetBytePosition.toLong)
    currentPosition += skippedBytes
  }

  private def playInternal(audioStream: AudioInputStream, line: SourceDataLine): Unit =
    soundStreamData.line.start()
    isStopped = false

    soundStreamData.audioStream.skip(currentPosition)
    val buffer = new Array[Byte](1024 * 4)
    var n = 0
    while (!isStopped && n != -1)
      n = audioStream.read(buffer, 0, buffer.length)
      if (n > 0)
        line.write(buffer, 0, n)
      currentPosition += n

object SoundService:
  val live = ZLayer.fromFunction(SoundService.apply _)
