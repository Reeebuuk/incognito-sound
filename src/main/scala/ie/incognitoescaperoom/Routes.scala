package ie.incognitoescaperoom

import ie.incognitoescaperoom.service.SoundServiceApi
import ie.incognitoescaperoom.settings.Settings
import zio.*
import zio.http.*
import zio.json.*

import scala.concurrent.Future

case class Routes(soundService: SoundServiceApi, settings: Settings):

  private def remapErrorAndLog[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, Nothing, A] =
    effect
      .catchAllCause { cause =>
        val logMsg = s"Error occurred: ${cause.prettyPrint}"
        ZIO.logError(logMsg) *> ZIO.fail(cause.squash)
      }
      .orElseFail(throw new RuntimeException("Unexpected error: should not be reached."))

  def routes: HttpApp[Any, Nothing] = Http
    .collectZIO[Request] {
      case Method.GET -> !! / "api" / "sound" / filename / "play" =>
        val value = soundService.getSound(filename).get
        for {
          _ <- remapErrorAndLog(ZIO.attempt(value.stop(true)))
          _ <- remapErrorAndLog(ZIO.attempt(value.play()))
        } yield Response.ok
      case Method.GET -> !! / "api" / "sound" / filename / "stop" =>
        for {
          _ <- remapErrorAndLog(ZIO.attempt(soundService.getSound(filename).get.stop(true)))
        } yield Response.ok
      case Method.GET -> !! / "api" / "sound" / filename / "pause" =>
        soundService.getSound(filename).get.stop()
        ZIO.unit.as(Response.ok)
      case Method.GET -> !! / "api" / "sound" / filename / "resume" =>
        for {
          _ <- remapErrorAndLog(ZIO.attempt(soundService.getSound(filename).get.stop()))
          _ <- remapErrorAndLog(ZIO.attempt(soundService.getSound(filename).get.play()))
        } yield Response.ok
      case Method.GET -> !! / "api" / "sound" / filename / "jump" / seconds =>
        for {
          _ <- remapErrorAndLog(ZIO.attempt(soundService.getSound(filename).get.jumpTo(seconds.toInt)))
        } yield Response.ok

    }

object Routes:
  val live = ZLayer.fromFunction(Routes.apply _)
