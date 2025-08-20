import com.peknight.build.gav.*
import com.peknight.build.sbt.*

commonSettings

lazy val acme = (project in file("."))
  .settings(name := "acme")
  .aggregate(
    acmeCore.jvm,
    acmeCore.js,
    acmeClient,
  )

lazy val acmeCore = (crossProject(JVMPlatform, JSPlatform) in file("acme-core"))
  .settings(name := "acme-core")
  .settings(crossDependencies(
    peknight.jose,
    peknight.http,
    peknight.codec.ip4s,
    peknight.security.bouncyCastle.provider,
    peknight.security.bouncyCastle.pkix,
    peknight.security.instances.codec,
    peknight.ext.catsEffect,
    peknight.ext.http4s,
  ))

lazy val acmeClient = (project in file("acme-client"))
  .settings(name := "acme-client")
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

lazy val acmeClientCore = (crossProject(JVMPlatform, JSPlatform) in file("acme-client/core"))
  .dependsOn(acmeCore)
  .settings(name := "acme-client-core")
  .settings(crossDependencies(peknight.ext.http4s))

lazy val acmeClientApi = (crossProject(JVMPlatform, JSPlatform) in file("acme-client/api"))
  .dependsOn(acmeClientCore)
  .settings(name := "acme-client-api")

lazy val acmeClientHttp4s = (crossProject(JVMPlatform, JSPlatform) in file("acme-client/http4s"))
  .dependsOn(
    acmeClientApi,
    acmeClientLetsEncrypt % Test,
    acmeClientCloudflare % Test,
  )
  .settings(name := "acme-client-http4s")
  .settings(crossDependencies(
    http4s.client,
    peknight.codec.http4s.circe,
    peknight.security.http4s,
    peknight.logging,
    peknight.commons.time,
  ))
  .settings(crossTestDependencies(
    peknight.logging.logback.config,
    peknight.cloudflare.zone.instances.codec,
    peknight.cloudflare.dns.record.http4s,
    http4s.dsl,
    http4s.ember.client,
    http4s.ember.server,
    scalaTest.flatSpec,
    typelevel.catsEffect.testingScalaTest,
  ))
  .jvmSettings(libraryDependencies ++= Seq(
    dependency(typelevel.log4Cats.slf4j),
    jvmTestDependency(logback.classic),
  ))

lazy val acmeClientLetsEncrypt = (crossProject(JVMPlatform, JSPlatform) in file("acme-client/lets-encrypt"))
  .dependsOn(acmeCore)
  .settings(name := "acme-client-lets-encrypt")

lazy val acmeClientCloudflare = (crossProject(JVMPlatform, JSPlatform) in file("acme-client/cloudflare"))
  .dependsOn(acmeClientApi)
  .settings(name := "acme-client-cloudflare")
  .settings(crossDependencies(
    peknight.cloudflare.dns.record.api,
    peknight.logging,
  ))
  .jvmSettings(libraryDependencies ++= dependencies(typelevel.log4Cats.slf4j))

lazy val acmeClientResource = (crossProject(JVMPlatform, JSPlatform) in file("acme-client/resource"))
  .dependsOn(acmeClientApi)
  .settings(name := "acme-client-resource")

lazy val acmeClientApp = (crossProject(JVMPlatform, JSPlatform) in file("acme-client/app"))
  .dependsOn(
    acmeClientResource,
    acmeClientHttp4s,
    acmeClientLetsEncrypt,
    acmeClientCloudflare,
  )
  .settings(name := "acme-client-app")
  .settings(crossDependencies(
    peknight.codec.effect,
    peknight.codec.fs2.io,
    peknight.cloudflare.dns.record.http4s,
    peknight.cloudflare.zone.instances.codec,
    http4s.ember.server,
    http4s.ember.client,
  ))
