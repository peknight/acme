ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.2"

ThisBuild / organization := "com.peknight"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Xfatal-warnings",
    "-language:strictEquality",
    "-Xmax-inlines:64"
  ),
)

lazy val acme = (project in file("."))
  .aggregate(
    acmeCore.jvm,
    acmeCore.js,
    acmeJose.jvm,
    acmeJose.js,
    acmeLetsEncrypt.jvm,
    acmeLetsEncrypt.js,
    acmeApi.jvm,
    acmeApi.js,
    acmeHttp4s.jvm,
    acmeHttp4s.js,
  )
  .settings(commonSettings)
  .settings(
    name := "acme",
  )

lazy val acmeCore = (crossProject(JSPlatform, JVMPlatform) in file("acme-core"))
  .settings(commonSettings)
  .settings(
    name := "acme-core",
    libraryDependencies ++= Seq(
      "com.peknight" %%% "jose-core" % pekJoseVersion,
      "com.peknight" %%% "codec-ip4s" % pekCodecVersion,
      "com.peknight" %%% "cats-effect-ext" % pekExtVersion,
      "com.peknight" %%% "http4s-ext" % pekExtVersion,
    ),
  )

lazy val acmeJose = (crossProject(JSPlatform, JVMPlatform) in file("acme-jose"))
  .settings(commonSettings)
  .settings(
    name := "acme-jose",
    libraryDependencies ++= Seq(
      "com.peknight" %%% "jose-core" % pekJoseVersion,
    ),
  )

lazy val acmeLetsEncrypt = (crossProject(JSPlatform, JVMPlatform) in file("acme-lets-encrypt"))
  .dependsOn(acmeCore)
  .settings(commonSettings)
  .settings(
    name := "acme-lets-encrypt",
    libraryDependencies ++= Seq(
    ),
  )

lazy val acmeApi = (crossProject(JSPlatform, JVMPlatform) in file("acme-api"))
  .dependsOn(acmeCore)
  .settings(commonSettings)
  .settings(
    name := "acme-api",
    libraryDependencies ++= Seq(
    ),
  )

lazy val acmeHttp4s = (crossProject(JSPlatform, JVMPlatform) in file("acme-http4s"))
  .dependsOn(
    acmeApi,
    acmeLetsEncrypt % Test,
  )
  .settings(commonSettings)
  .settings(
    name := "acme-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-client" % http4sVersion,
      "com.peknight" %%% "codec-http4s-circe" % pekCodecVersion,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion % Test,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % catsEffectTestingScalaTestVersion % Test,
    ),
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      log4CatsSlf4j % Test,
      pekLogbackConfig % Test,
      logbackClassic % Test,
    ),
  )

val http4sVersion = "1.0.0-M34"
val catsEffectTestingScalaTestVersion = "1.6.0"
val log4CatsVersion = "2.7.0"
val logbackVersion = "1.5.16"
val pekVersion = "0.1.0-SNAPSHOT"
val pekCodecVersion = pekVersion
val pekJoseVersion = pekVersion
val pekExtVersion = pekVersion
val pekErrorVersion = pekVersion
val pekLoggingVersion = pekVersion

val log4CatsSlf4j = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion
val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
val pekLogbackConfig = "com.peknight" %% "logback-config" % pekLoggingVersion
