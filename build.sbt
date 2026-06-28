import com.peknight.build.gav.*
import com.peknight.build.sbt.*

commonSettings

lazy val acme = (project in file("."))
  .settings(name := "acme")
  .aggregate(acmeCore.projectRefs *)
  .aggregate(acmeClientCore.projectRefs *)
  .aggregate(acmeClientApi.projectRefs *)
  .aggregate(acmeClientHttp4s.projectRefs *)
  .aggregate(acmeClientLetsEncrypt.projectRefs *)
  .aggregate(acmeClientCloudflare.projectRefs *)
  .aggregate(acmeClientStream.projectRefs *)
  .aggregate(acmeClientApp.projectRefs *)

lazy val acmeCore = (projectMatrix in file("acme-core"))
  .settings(name := "acme-core")
  .settings(libraryDependencies ++= dependencies(
    peknight.jose,
    peknight.http,
    peknight.codec.ip4s,
    peknight.catsEffect,
    peknight.http4s,
  ))
  .jvmPlatform(
    scalaVersions = Seq(scala.scala3.version),
    settings = Seq(
      libraryDependencies ++= dependencies(
        peknight.security.bouncyCastle.provider,
        peknight.security.bouncyCastle.pkix,
        peknight.security.instances.codec,
      )
    )
  )
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val acmeClientCore = (projectMatrix in file("acme-client/core"))
  .dependsOn(acmeCore)
  .settings(name := "acme-client-core")
  .settings(libraryDependencies ++= dependencies(
    peknight.codec.effect,
    peknight.http4s,
  ))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val acmeClientApi = (projectMatrix in file("acme-client/api"))
  .dependsOn(acmeClientCore)
  .settings(name := "acme-client-api")
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val acmeClientHttp4s = (projectMatrix in file("acme-client/http4s"))
  .dependsOn(
    acmeClientApi,
    acmeClientLetsEncrypt % Test,
    acmeClientCloudflare % Test,
  )
  .settings(name := "acme-client-http4s")
  .settings(libraryDependencies ++= dependencies(
    http4s.client,
    peknight.codec.http4s.circe,
    peknight.security.http4s,
    peknight.logging,
    peknight.commons.time,
  ))
  .settings(libraryDependencies ++= testDependencies(
    peknight.logging.logback.config,
    peknight.cloudflare.zone.config,
    peknight.cloudflare.dns.record.http4s,
    http4s.dsl,
    http4s.ember.client,
    http4s.ember.server,
    scalaTest.flatSpec,
    typelevel.catsEffect.testingScalaTest,
  ))
  .jvmPlatform(
    scalaVersions = Seq(scala.scala3.version),
    settings = Seq(
      libraryDependencies ++= dependencies(typelevel.log4Cats.slf4j),
      libraryDependencies ++= jvmTestDependencies(logback.classic),
    )
  )
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val acmeClientLetsEncrypt = (projectMatrix in file("acme-client/lets-encrypt"))
  .dependsOn(acmeCore)
  .settings(name := "acme-client-lets-encrypt")
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val acmeClientCloudflare = (projectMatrix in file("acme-client/cloudflare"))
  .dependsOn(acmeClientApi)
  .settings(name := "acme-client-cloudflare")
  .settings(libraryDependencies ++= dependencies(
    peknight.cloudflare.dns.record.api,
    peknight.logging,
  ))
  .jvmPlatform(
    scalaVersions = Seq(scala.scala3.version),
    settings = Seq(
      libraryDependencies ++= dependencies(typelevel.log4Cats.slf4j),
    )
  )
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val acmeClientStream = (projectMatrix in file("acme-client/stream"))
  .dependsOn(acmeClientApi)
  .settings(name := "acme-client-stream")
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))

lazy val acmeClientApp = (projectMatrix in file("acme-client/app"))
  .dependsOn(acmeClientStream, acmeClientHttp4s, acmeClientLetsEncrypt, acmeClientCloudflare)
  .settings(name := "acme-client-app")
  .settings(libraryDependencies ++= dependencies(
    peknight.codec.fs2.io,
    peknight.cloudflare.zone.config,
    peknight.cloudflare.dns.record.http4s,
    http4s.ember.server,
    http4s.ember.client,
  ))
  .jvmPlatform(scalaVersions = Seq(scala.scala3.version))
  .jsPlatform(scalaVersions = Seq(scala.scala3.version))
