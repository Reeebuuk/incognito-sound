package ie.incognitoescaperoom

import zio.*
import zio.http.*
import zio.http.Header.AccessControlAllowMethods
import zio.http.internal.middlewares.Cors
import zio.json.{ DecoderOps, EncoderOps }
import zio.logging.*
import zio.logging.LogFormat.*
import zio.logging.backend.SLF4J

import java.io.IOException
import java.util.concurrent.TimeUnit

object Main extends ZIOAppDefault with Layers:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private def app = for {
    _      <- ZIO.logInfo("App starting up on 9000")
    routes <- ZIO.service[Routes]
    _      <- server(routes).as(ZIO.never)
  } yield ()

  override val run: UIO[ExitCode] =
    app
      .provide(env)
      .exitCode

  val corsConfig =
    Cors.CorsConfig(allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.PUT))

  def httpRoutes(routes: Routes) = routes.routes @@
    HttpAppMiddleware.cors(corsConfig) @@
    HttpAppMiddleware.timeout(5.seconds) @@
    HttpAppMiddleware.debug

  def server(routes: Routes): ZIO[Any, Throwable, Nothing] =
    Server
      .serve(httpRoutes(routes))
      .provide(Server.defaultWith(_.port(9000).enableRequestStreaming))
      .onError { x =>
        ZIO.foreach(x.failures)(err => ZIO.logErrorCause(s"Uncaught error ${err.getLocalizedMessage}", Cause.fail(x)))
      }
