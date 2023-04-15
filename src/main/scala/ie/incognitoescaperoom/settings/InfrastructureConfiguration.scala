package ie.incognitoescaperoom.settings

import zio._

final class InfrastructureConfiguration() {

  lazy val settings: Settings = Settings.conf

}

object InfrastructureConfiguration {

  def apply(): InfrastructureConfiguration =
    new InfrastructureConfiguration()

  val settings: URIO[InfrastructureConfiguration, Settings] =
    ZIO.environmentWith(_.get.settings)

  val live: ULayer[InfrastructureConfiguration] =
    ZLayer.succeed[InfrastructureConfiguration](InfrastructureConfiguration())

}
