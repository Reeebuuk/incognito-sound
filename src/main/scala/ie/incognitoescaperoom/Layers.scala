package ie.incognitoescaperoom

import ie.incognitoescaperoom.service.*
import ie.incognitoescaperoom.settings.{ InfrastructureConfiguration, Settings }
import ie.incognitoescaperoom.service.{ SoundService, SoundServiceApi }
import ie.incognitoescaperoom.settings.{ InfrastructureConfiguration, Settings }
import zio.*
import zio.http.Response

import java.util.concurrent.Executors
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

trait Layers:

  val service = ZLayer.makeSome[Settings, SoundServiceApi](SoundService.live)

  val settingsLayer: TaskLayer[Settings] = InfrastructureConfiguration.live >>> ZLayer.scoped(InfrastructureConfiguration.settings)

  val liveRoutes: ZLayer[Any, Any, Routes] =
    settingsLayer >>> (settingsLayer ++ service) >>> Routes.live

  val env = liveRoutes
