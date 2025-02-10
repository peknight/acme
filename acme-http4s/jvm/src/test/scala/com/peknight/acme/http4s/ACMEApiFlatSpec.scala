package com.peknight.acme.http4s

import cats.data.EitherT
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Ref}
import cats.syntax.option.*
import com.peknight.acme.Directory
import com.peknight.acme.http.HttpCache
import com.peknight.acme.letsencrypt.uri.stagingDirectory
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.syntax.applicativeError.asError
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
        either <- EmberClientBuilder.default[IO].withLogger(logger).withTimeout(10.seconds).build
          .use { client =>
            val eitherT =
              for
                locale <- EitherT(IO.blocking(Locale.getDefault).asError)
                nonceRef <- EitherT(Ref[IO].of(none[Base64UrlNoPad]).asError)
                directoryRef <- EitherT(Ref[IO].of(none[HttpCache[Directory]]).asError)
                api = ACMEApi[IO](locale, true, nonceRef, directoryRef)(client)(dsl.io)
                directory <- EitherT(api.directory(stagingDirectory))
                _ <- EitherT(info"directory: $directory".asError)
                _ <- EitherT(api.resetNonce(directory.newNonce))
                nonce <- EitherT(nonceRef.get.asError)
                _ <- EitherT(info"nonce: $nonce".asError)
              yield
                directory
            eitherT.value
          }
      yield either
    run.asserting(either => assert(either.isRight))
  }
end ACMEApiFlatSpec
