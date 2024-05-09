package com.peknight.acme.http4s

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.peknight.acme.letsencrypt.uri.stagingDirectory
import org.http4s.*
import org.http4s.client.dsl
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AsyncFlatSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.syntax.*

import java.util.Locale
import scala.concurrent.duration.*

class AcmeApiFlatSpec extends AsyncFlatSpec with AsyncIOSpec:
  "ACME Api Directory" should "succeed" in {
    for
      logger <- Slf4jLogger.fromClass[IO](classOf[AcmeApiFlatSpec])
      given Logger[IO] = logger
      res <- EmberClientBuilder.default[IO].withLogger(logger).withTimeout(10.seconds).build
        .use { client =>
          for
            locale <- IO.blocking(Locale.getDefault)
            result <- ACMEApi[IO](locale, true)(client)(dsl.io).directory(stagingDirectory)(None)
            _ <- info"directory result: $result"
          yield
            result
        }
        .asserting { result =>
          assert(result.body.isDefined)
        }
    yield res
  }
end AcmeApiFlatSpec
