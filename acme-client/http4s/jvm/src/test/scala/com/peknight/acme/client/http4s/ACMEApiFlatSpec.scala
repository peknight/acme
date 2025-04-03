package com.peknight.acme.client.http4s

import cats.data.EitherT
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import cats.{Id, Show}
import com.peknight.acme.client.cloudflare.DNSChallengeClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.acme.client.letsencrypt.uri
import com.peknight.acme.client.letsencrypt.uri.stagingDirectory
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.cloudflare.test.{pekToken, pekZoneId}
import com.peknight.codec.syntax.encoder.asS
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.jose.jwk.JsonWebKey
import com.peknight.logging.syntax.eitherT.log
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.bouncycastle.openssl.PEMParser
import com.peknight.security.bouncycastle.openssl.jcajce.JcaPEMWriter
import com.peknight.security.bouncycastle.pkix.syntax.jcaPEMKeyConverter.getKeyPairF
import com.peknight.security.bouncycastle.pkix.syntax.jcaPEMWriter.writeObjectF
import com.peknight.security.bouncycastle.pkix.syntax.pemParser.readObjectF
import com.peknight.security.ecc.sec.secp256r1
import fs2.io.file.Path
import io.circe.Json
import org.bouncycastle.openssl.jcajce.{JcaPEMKeyConverter, JcaPEMWriter as JJcaPEMWriter}
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser as JPEMParser}
import org.http4s.*
import org.http4s.client.dsl
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AsyncFlatSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.security.KeyPair
import scala.concurrent.duration.*

class ACMEApiFlatSpec extends AsyncFlatSpec with AsyncIOSpec:

  "ACME Api Directory" should "succeed" in {
    given Show[KeyPair] = Show.show(keyPair =>
      JsonWebKey.fromKeyPair(keyPair).map(_.asS[Id, Json].deepDropNullValues.noSpaces).getOrElse(keyPair.toString)
    )
    given [A]: Show[A] = Show.fromToString[A]
    given CanEqual[JsonWebKey, JsonWebKey] = CanEqual.derived
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
                acmeClient <- EitherT(ACMEClient[IO, Challenge](client, stagingDirectory)(dsl.io).asError)
                given DNSRecordApi[IO] = DNSRecordApi[IO](pekToken)(client)(dsl.io)
                dnsChallengeClient <- EitherT(DNSChallengeClient[IO](pekZoneId).asError)
                // test
                keyPair <- EitherT(secp256r1.generateKeyPair[IO](provider = Some(provider)).asError)
                  .log(name = "generateKeyPair")
                _ <- EitherT(Resource.fromAutoCloseable[IO, JJcaPEMWriter](JcaPEMWriter[IO](Path("logs/account.key")))
                  .use(_.writeObjectF[IO](keyPair)).asError).log(name = "writeKeyPair")
                pemKeyPair <- EitherT(Resource.fromAutoCloseable[IO, JPEMParser](PEMParser[IO](Path("logs/account.key")))
                  .use(_.readObjectF[IO, PEMKeyPair]).asError.map(_.flatten)).log(name = "readKeyPair")
                keyPair2 <- EitherT(JcaPEMKeyConverter().getKeyPairF[IO](pemKeyPair).asError)
                jwk1 <- JsonWebKey.fromKeyPair(keyPair).eLiftET[IO]
                jwk2 <- JsonWebKey.fromKeyPair(keyPair2).eLiftET[IO]
                _ <- EitherT(IO.println(jwk1 == jwk2).asError)
//                identifiers <- NonEmptyList.of(
//                  "*.peknight.com",
//                  "*.server.peknight.com",
//                  "*.ctrl.peknight.com",
//                  "*.cdn.peknight.com"
//                ).traverse(domain => Identifier.dns(domain).eLiftET[IO])
//                certificates <- EitherT(acmeClient.fetchCertificate[DNS, `dns-01`, DNSRecordId](identifiers,
//                  secp256r1.generateKeyPair[IO](provider = Some(provider)).asError,
//                  RSA.keySizeGenerateKeyPair[IO](4096).asError)(
//                  acmeClient.getDnsIdentifierAndChallenge)(dnsChallengeClient.createDNSRecord)(
//                  dnsChallengeClient.cleanDNSRecord(_, _, _).map(_.as(()))
//                )).log(name = "fetchCertificates", param = Some(identifiers))
              yield
                ()
            eitherT.value
          }
      yield either
    run.asserting(either => assert(either.isRight))
  }
end ACMEApiFlatSpec
