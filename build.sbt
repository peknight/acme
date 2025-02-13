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
    acmeClient,
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

lazy val acmeServerCore = (crossProject(JSPlatform, JVMPlatform) in file("acme-server/core"))
  .dependsOn(acmeCore)
  .settings(commonSettings)
  .settings(
    name := "acme-server-core",
    libraryDependencies ++= Seq(
    ),
  )

lazy val acmeClient = (project in file("acme-client"))
  .aggregate(
    acmeClientCore.jvm,
    acmeClientCore.js,
    acmeClientApi.jvm,
    acmeClientApi.js,
    acmeClientHttp4s.jvm,
    acmeClientHttp4s.js,
    acmeClientLetsEncrypt.jvm,
    acmeClientLetsEncrypt.js,
  )
  .settings(commonSettings)
  .settings(
    name := "acme-client",
  )

lazy val acmeClientCore = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/core"))
  .dependsOn(acmeCore)
  .settings(commonSettings)
  .settings(
    name := "acme-client-core",
    libraryDependencies ++= Seq(
      "com.peknight" %%% "http4s-ext" % pekExtVersion,
    ),
  )

lazy val acmeClientApi = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/api"))
  .dependsOn(acmeClientCore)
  .settings(commonSettings)
  .settings(
    name := "acme-client-api",
    libraryDependencies ++= Seq(
    ),
  )

lazy val acmeClientHttp4s = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/http4s"))
  .dependsOn(
    acmeClientApi,
    acmeClientLetsEncrypt % Test,
  )
  .settings(commonSettings)
  .settings(
    name := "acme-client-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-client" % http4sVersion,
      "com.peknight" %%% "codec-http4s-circe" % pekCodecVersion,
      "com.peknight" %%% "security-bcprov" % pekSecurityVersion % Test,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion % Test,
      "org.scalatest" %%% "scalatest-flatspec" % scalaTestVersion % Test,
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

lazy val acmeClientLetsEncrypt = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/lets-encrypt"))
  .dependsOn(acmeClientCore)
  .settings(commonSettings)
  .settings(
    name := "acme-client-lets-encrypt",
    libraryDependencies ++= Seq(
    ),
  )

val http4sVersion = "1.0.0-M34"
val log4CatsVersion = "2.7.0"
val logbackVersion = "1.5.16"
val scalaTestVersion = "3.2.19"
val catsEffectTestingScalaTestVersion = "1.6.0"
val pekVersion = "0.1.0-SNAPSHOT"
val pekCodecVersion = pekVersion
val pekSecurityVersion = pekVersion
val pekJoseVersion = pekVersion
val pekExtVersion = pekVersion
val pekErrorVersion = pekVersion
val pekLoggingVersion = pekVersion

val log4CatsSlf4j = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion
val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
val pekLogbackConfig = "com.peknight" %% "logback-config" % pekLoggingVersion
