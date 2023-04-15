package ie.incognitoescaperoom.settings

import com.typesafe.config.ConfigFactory
import pureconfig.*
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.derivation.default.*
import zio.*

case class SoundSettings(filename: String, deviceName: String, isBackgroundMusic: Boolean) derives ConfigReader

case class Settings(sounds: List[SoundSettings]) derives ConfigReader

object Settings:
  val conf: Settings = ConfigSource.fromConfig(ConfigFactory.load()).loadOrThrow[Settings]
