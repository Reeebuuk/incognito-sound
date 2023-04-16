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
        for {
          _     <- ZIO.attempt(soundService.getSound(filename).get.stop(true)).orDie
          fiber <- ZIO.attemptBlocking(soundService.getSound(filename).get.play()).forkDaemon
          _     <- ZIO.attempt(soundService.setPlayingFiber(filename, fiber)).orDie
        } yield Response.ok
      case Method.GET -> !! / "api" / "sound" / filename / "stop" =>
        for {
          x <- remapErrorAndLog(ZIO.attempt(soundService.getSound(filename).get.stop(true)))
        } yield Response.ok
      case Method.GET -> !! / "api" / "sound" / filename / "pause" =>
        soundService.getSound(filename).get.stop()
        ZIO.unit.as(Response.ok)
      case Method.GET -> !! / "api" / "sound" / filename / "resume" =>
        for {
          _     <- ZIO.attempt(soundService.getSound(filename).get.stop()).orDie
          fiber <- ZIO.attemptBlocking(soundService.getSound(filename).get.play()).forkDaemon
          _     <- ZIO.attempt(soundService.setPlayingFiber(filename, fiber)).orDie
        } yield Response.ok
      case Method.GET -> !! / "api" / "sound" / filename / "jump" / seconds =>
        for {
          fiber <- ZIO.attemptBlocking(soundService.getSound(filename).get.jumpTo(seconds.toInt)).forkDaemon
          _     <- ZIO.attempt(soundService.setPlayingFiber(filename, fiber)).orDie
        } yield Response.ok

    }

object Routes:
  val live = ZLayer.fromFunction(Routes.apply _)
