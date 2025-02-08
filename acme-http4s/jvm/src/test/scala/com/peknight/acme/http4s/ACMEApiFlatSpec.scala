package com.peknight.acme.http4s

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Ref}
import cats.syntax.option.*
import com.peknight.acme.Directory
import com.peknight.acme.letsencrypt.uri.stagingDirectory
import com.peknight.codec.base.Base64UrlNoPad
import org.http4s.*
import org.http4s.client.dsl
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AsyncFlatSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.syntax.*

import java.util.Locale
import scala.concurrent.duration.*

class ACMEApiFlatSpec extends AsyncFlatSpec with AsyncIOSpec:
  "ACME Api Directory" should "succeed" in {
    val run =
      for
        logger <- Slf4jLogger.fromClass[IO](classOf[ACMEApiFlatSpec])
        given Logger[IO] = logger
        directory <- EmberClientBuilder.default[IO].withLogger(logger).withTimeout(10.seconds).build
          .use { client =>
            for
              locale <- IO.blocking(Locale.getDefault)
              directoryRef <- Ref[IO].of(none[(Headers, Directory)])
              nonceRef <- Ref[IO].of(none[Base64UrlNoPad])
              either <- ACMEApi[IO](locale, true, directoryRef, nonceRef)(client)(dsl.io).directory(stagingDirectory)
              _ <- info"directory result: $either"
            yield
              either
          }
      yield directory
    run.asserting(either => assert(either.isRight))
  }
end ACMEApiFlatSpec
