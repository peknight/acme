package com.peknight.acme.client.http4s

import cats.data.EitherT
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.peknight.acme.account.AccountClaims
import com.peknight.acme.client.letsencrypt.uri
import com.peknight.acme.client.letsencrypt.uri.stagingDirectory
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.cipher.RSA
import com.peknight.security.ecc.sec.secp256r1
import org.http4s.*
import org.http4s.client.dsl
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AsyncFlatSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.syntax.*

import scala.concurrent.duration.*

class ACMEApiFlatSpec extends AsyncFlatSpec with AsyncIOSpec:
  "ACME Api Directory" should "succeed" in {
    val run =
      for
        provider <- BouncyCastleProvider[IO]
        _ <- Security.addProvider[IO](provider)
        logger <- Slf4jLogger.fromClass[IO](classOf[ACMEApiFlatSpec])
        given Logger[IO] = logger
        either <- EmberClientBuilder.default[IO].withLogger(logger).withTimeout(10.seconds).build
          .use { client =>
            val eitherT =
              for
                acmeClient <- EitherT(ACMEClient[IO](client, stagingDirectory)(dsl.io).asError)
                userKeyPair <- EitherT(secp256r1.generateKeyPair[IO](provider = Some(provider)).asError)
                account <- EitherT(acmeClient.newAccount(AccountClaims(termsOfServiceAgreed = Some(true)), userKeyPair))
                _ <- EitherT(info"account: $account".asError)
                domainKeyPair <- EitherT(RSA.keySizeGenerateKeyPair[IO](4096).asError)
              yield
                account
            eitherT.value
          }
      yield either
    run.asserting(either => assert(either.isRight))
  }
end ACMEApiFlatSpec
