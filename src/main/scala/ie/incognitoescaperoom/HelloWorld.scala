package ie.incognitoescaperoom

import zio._
import zio.http._

object HelloWorld extends ZIOAppDefault {

  val app: HttpApp[Any, Nothing] = Http.collect[Request] { case Method.GET -> !! / "text" =>
    Response.text("Hello World!")
  }

  override val run =
    Server.serve(app).provide(Server.default)
}
