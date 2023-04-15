package ie.incognitoescaperoom

import org.slf4j.{ LoggerFactory, MDC }
import zio.*
import zio.http.middleware.Cors
import zio.http.model.Method
import zio.http.{ HttpApp, HttpAppMiddleware, Server, ServerConfig }
import zio.json.{ DecoderOps, EncoderOps }
import zio.logging.*
import zio.logging.LogFormat.*
import zio.logging.backend.SLF4J

import java.io.IOException
import java.util.concurrent.TimeUnit

object Main extends ZIOAppDefault with Layers:

  private def app = for {
    _      <- ZIO.logInfo("App starting up")
    routes <- ZIO.service[Routes]
    fiber  <- server(routes).forkDaemon
    _      <- ZIO.never
  } yield fiber

  override val run: UIO[ExitCode] =
    app
      .provide(env)
      .flatMap(_.join)
      .exitCode

  val corsConfig =
    Cors.CorsConfig(allowedOrigins = _ == "*", allowedMethods = Some(Set(Method.PUT, Method.DELETE, Method.POST, Method.GET)))

  def httpRoutes(routes: Routes) = routes.routes @@
    HttpAppMiddleware.cors(corsConfig) @@
    HttpAppMiddleware.timeout(5.seconds) @@
    HttpAppMiddleware.debug

  val config: ServerConfig =
    ServerConfig.default
      .port(9000)
      .maxThreads(2)

  def server(routes: Routes): ZIO[Any, Throwable, Nothing] = Server
    .serve(httpRoutes(routes))
    .provide(ServerConfig.live(config), Server.live)
