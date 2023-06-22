ThisBuild / scalaVersion := "3.3.0"
ThisBuild / version := "0.0.1-SNAPSHOT"
ThisBuild / organization := "ie.incognitoescaperoom"
ThisBuild / organizationName := "incognito-sound"
ThisBuild / Test / fork := true

val doobieVersion = "1.0.0-RC4"
val zioVersion    = "2.0.15"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "incognito-sound",
    libraryDependencies ++= Seq(
      "dev.zio"               %% "zio"                      % zioVersion,
      "dev.zio"               %% "zio-http"                 % "3.0.0-RC2",
      "dev.zio"               %% "zio-json"                 % "0.5.0",
      "dev.zio"               %% "zio-logging"              % "2.1.13",
      "dev.zio"               %% "zio-logging-slf4j2"       % "2.1.13",
      "ch.qos.logback"         % "logback-classic"          % "1.4.8",
      "net.logstash.logback"   % "logstash-logback-encoder" % "7.4",
      "org.codehaus.janino"    % "janino"                   % "3.1.9",
      "com.github.pureconfig" %% "pureconfig-core"          % "0.17.4",
      "org.scalatest"         %% "scalatest"                % "3.2.16"   % Test,
      "dev.zio"               %% "zio-test"                 % zioVersion % Test,
      "dev.zio"               %% "zio-test-sbt"             % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    maintainer := "reeebuuk@gmail.com"
  )
