ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.0"

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
      "com.peknight" %%% "http-core" % pekHttpVersion,
      "com.peknight" %%% "codec-ip4s" % pekCodecVersion,
      "com.peknight" %%% "security-bcprov" % pekSecurityVersion,
      "com.peknight" %%% "security-bcpkix" % pekSecurityVersion,
      "com.peknight" %%% "security-codec-instances" % pekSecurityVersion,
      "com.peknight" %%% "cats-effect-ext" % pekExtVersion,
      "com.peknight" %%% "http4s-ext" % pekExtVersion,
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
    acmeClientCloudflare.jvm,
    acmeClientCloudflare.js,
    acmeClientResource.jvm,
    acmeClientResource.js,
    acmeClientApp.jvm,
    acmeClientApp.js,
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
    acmeClientCloudflare % Test,
  )
  .settings(commonSettings)
  .settings(
    name := "acme-client-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-client" % http4sVersion,
      "com.peknight" %%% "codec-http4s-circe" % pekCodecVersion,
      "com.peknight" %%% "security-http4s" % pekSecurityVersion,
      "com.peknight" %%% "logging-core" % pekLoggingVersion,
      "com.peknight" %%% "commons-time" % pekCommonsVersion,
      "com.peknight" %%% "logback-config" % pekLoggingVersion % Test,
      "com.peknight.cloudflare" %%% "zone-codec-instances" % pekCloudflareVersion % Test,
      "com.peknight.cloudflare" %%% "dns-record-http4s" % pekCloudflareVersion % Test,
      "org.http4s" %%% "http4s-dsl" % http4sVersion % Test,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion % Test,
      "org.http4s" %%% "http4s-ember-server" % http4sVersion % Test,
      "org.scalatest" %%% "scalatest-flatspec" % scalaTestVersion % Test,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % catsEffectTestingScalaTestVersion % Test,
    ),
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      log4CatsSlf4j,
      logbackClassic % Test,
    ),
  )

lazy val acmeClientLetsEncrypt = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/lets-encrypt"))
  .dependsOn(acmeCore)
  .settings(commonSettings)
  .settings(
    name := "acme-client-lets-encrypt",
    libraryDependencies ++= Seq(
    ),
  )

lazy val acmeClientCloudflare = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/cloudflare"))
  .dependsOn(acmeClientApi)
  .settings(commonSettings)
  .settings(
    name := "acme-client-cloudflare",
    libraryDependencies ++= Seq(
      "com.peknight.cloudflare" %%% "dns-record-api" % pekCloudflareVersion,
      "com.peknight" %%% "logging-core" % pekLoggingVersion,
    ),
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      log4CatsSlf4j,
    )
  )

lazy val acmeClientResource = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/resource"))
  .dependsOn(
    acmeClientApi,
  )
  .settings(commonSettings)
  .settings(
    name := "acme-client-resource",
    libraryDependencies ++= Seq(
    ),
  )

lazy val acmeClientApp = (crossProject(JSPlatform, JVMPlatform) in file("acme-client/app"))
  .dependsOn(
    acmeClientResource,
    acmeClientHttp4s,
    acmeClientLetsEncrypt,
    acmeClientCloudflare,
  )
  .settings(commonSettings)
  .settings(
    name := "acme-client-app",
    libraryDependencies ++= Seq(
      "com.peknight" %%% "codec-effect" % pekCodecVersion,
      "com.peknight" %%% "codec-fs2-io" % pekCodecVersion,
      "com.peknight.cloudflare" %%% "dns-record-http4s" % pekCloudflareVersion,
      "com.peknight.cloudflare" %%% "zone-codec-instances" % pekCloudflareVersion,
      "org.http4s" %%% "http4s-ember-server" % http4sVersion,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion,
    ),
  )

val http4sVersion = "1.0.0-M34"
val log4CatsVersion = "2.7.0"
val logbackVersion = "1.5.18"
val scalaTestVersion = "3.2.19"
val catsEffectTestingScalaTestVersion = "1.6.0"
val pekVersion = "0.1.0-SNAPSHOT"
val pekCodecVersion = pekVersion
val pekCommonsVersion = pekVersion
val pekHttpVersion = pekVersion
val pekSecurityVersion = pekVersion
val pekJoseVersion = pekVersion
val pekCloudflareVersion = pekVersion
val pekExtVersion = pekVersion
val pekErrorVersion = pekVersion
val pekMethodVersion = pekVersion
val pekLoggingVersion = pekVersion

val log4CatsSlf4j = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion
val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
val pekLogbackConfig = "com.peknight" %% "logback-config" % pekLoggingVersion
