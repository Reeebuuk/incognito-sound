package ie.incognitoescaperoom

import ie.incognitoescaperoom.service.SoundServiceApi
import ie.incognitoescaperoom.settings.Settings
import zio.*
import zio.http.*
import zio.json.*

case class Routes(soundService: SoundServiceApi, settings: Settings):

  def routes: HttpApp[Any, Nothing] = Http
    .collect[Request] { case Method.GET -> !! / "api" / "sound" / filename / "play" =>
      soundService.getSound(filename) match {
        case Some(sound) =>
          sound.play()
          Response.ok
        case None =>
          Response.ok.copy(status = Status.BadRequest)
      }
    }

object Routes:
  val live = ZLayer.fromFunction(Routes.apply _)
