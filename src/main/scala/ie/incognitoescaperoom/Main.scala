package ie.incognitoescaperoom

import org.slf4j.{ LoggerFactory, MDC }
import zio.*
import zio.http.Header.AccessControlAllowMethods
import zio.http.internal.middlewares.Cors
import zio.http.{ HttpApp, HttpAppMiddleware, Method, Server }
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
    Cors.CorsConfig(allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.PUT))

  def httpRoutes(routes: Routes) = routes.routes @@
    HttpAppMiddleware.cors(corsConfig) @@
    HttpAppMiddleware.timeout(5.seconds) @@
    HttpAppMiddleware.debug

  def server(routes: Routes): ZIO[Any, Throwable, Nothing] =
    Server.serve(httpRoutes(routes)).provide(Server.defaultWith(_.port(9000).enableRequestStreaming))
