ThisBuild / scalaVersion := "3.3.0-RC2"
ThisBuild / version := "0.0.1-SNAPSHOT"
ThisBuild / organization := "ie.incognitoescaperoom"
ThisBuild / organizationName := "incognito-sound"
ThisBuild / Test / fork := true

val doobieVersion = "1.0.0-RC2"
val zioVersion    = "2.0.10"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "incognito-sound",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"      % zioVersion,
      "dev.zio" %% "zio-http" % "0.0.5",
      "dev.zio" %% "zio-json" % "0.5.0",
//      "dev.zio"               %% "zio-logging"                       % "2.1.0",
      "dev.zio"               %% "zio-logging-slf4j"        % "2.1.11",
      "com.github.pureconfig" %% "pureconfig-core"          % "0.17.2",
      "ch.qos.logback"         % "logback-classic"          % "1.4.6",
      "net.logstash.logback"   % "logstash-logback-encoder" % "7.3",
      "org.codehaus.janino"    % "janino"                   % "3.1.9",
      "org.scalatest"         %% "scalatest"                % "3.2.15"   % Test,
      "dev.zio"               %% "zio-test"                 % zioVersion % Test,
      "dev.zio"               %% "zio-test-sbt"             % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    maintainer := "reeebuuk@gmail.com"
  )
