package ie.incognitoescaperoom

import ie.incognitoescaperoom.service.*
import ie.incognitoescaperoom.settings.{ InfrastructureConfiguration, Settings }
import ie.incognitoescaperoom.service.{ SoundService, SoundServiceApi }
import ie.incognitoescaperoom.settings.{ InfrastructureConfiguration, Settings }
import zio.*
import zio.http.Response
import zio.logging.backend.SLF4J

import java.util.concurrent.Executors
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

trait Layers:

  val service =
    ZLayer
      .makeSome[Settings, SoundServiceApi](SoundService.live)

  private val executorService = Executors.newFixedThreadPool(10)
  private val executorContextLayer: ULayer[ExecutionContext] =
    ZLayer.succeed(ExecutionContext.fromExecutor(executorService))

  val settingsLayer: TaskLayer[Settings] = InfrastructureConfiguration.live >>> ZLayer.scoped(InfrastructureConfiguration.settings)

  val liveRoutes: ZLayer[Any, Any, Routes] =
    (settingsLayer ++ executorContextLayer) >>> (settingsLayer ++ service) >>> Routes.live

  val loggingLayer: ZLayer[Any, Nothing, Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val env = liveRoutes ++ loggingLayer
