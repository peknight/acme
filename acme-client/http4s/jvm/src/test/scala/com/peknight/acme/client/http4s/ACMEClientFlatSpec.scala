package com.peknight.acme.client.http4s

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.option.*
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.client.cloudflare.CloudflareDNSChallengeClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.acme.client.letsencrypt.uri.{acmeStaging, resolve}
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.cloudflare.test.{pekToken, pekZoneId}
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.logging.syntax.eitherT.log
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.bouncycastle.openssl.{fetchKeyPair, fetchX509CertificatesAndKeyPair}
import com.peknight.security.cipher.RSA
import com.peknight.security.ecc.sec.secp256r1
import fs2.io.file.Path
import org.http4s.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AsyncFlatSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

class ACMEClientFlatSpec extends AsyncFlatSpec with AsyncIOSpec:

  "ACME Client" should "succeed" in {
    val run =
      for
        provider <- BouncyCastleProvider[IO]
        _ <- Security.addProvider[IO](provider)
        logger <- Slf4jLogger.fromClass[IO](classOf[ACMEClientFlatSpec])
        given Logger[IO] = logger
        either <- EmberClientBuilder.default[IO].withLogger(logger).withTimeout(10.seconds).build
          .use { client =>
            given Client[IO] = client
            val eitherT =
              for
                stagingDirectory <- resolve(acmeStaging).eLiftET[IO]
                acmeClient <- EitherT(ACMEClient[IO, Challenge](stagingDirectory).asError)
                given DNSRecordApi[IO] = DNSRecordApi[IO](pekToken)
                dnsChallengeClient <- EitherT(CloudflareDNSChallengeClient[IO, Challenge](pekZoneId).asError)
                given CloudflareDNSChallengeClient[IO, Challenge] = dnsChallengeClient
                accountKeyPair <- EitherT(fetchKeyPair[IO](Path("cert/account.key"))(
                  secp256r1.generateKeyPair[IO](provider = provider.some).asError))
                certificates <- EitherT(fetchX509CertificatesAndKeyPair[IO](Path("cert/domain.crt"),
                  Path("cert/domain.key"), provider.some, provider.some) {
                  val et =
                    for
                      identifiers <- NonEmptyList.of(
                        "*.peknight.com",
                      ).traverse(domain => Identifier.dns(domain).eLiftET[IO])
                      context <- EitherT(acmeClient.fetchCertificate[DNS, `dns-01`, DNSRecordId](
                        identifiers,
                        accountKeyPair.asRight.pure,
                        RSA.keySizeGenerateKeyPair[IO](4096, provider = provider.some).asError
                      ))
                    yield
                      (context.certificates, context.domainKeyPair)
                  et.value
                })
              yield
                ()
            eitherT.log[Unit]("ACMEClientFlatSpec#run").value
          }
      yield either
    run.asserting(either => assert(either.isRight))
  }

  "Fetch KeyPair" should "succeed" in {
    import cats.Monad
    import com.peknight.error.Error
    import com.peknight.error.std.WrongClassTag
    import com.peknight.security.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
    import com.peknight.security.bouncycastle.openssl.{readPEM, writePEM}
    import com.peknight.security.bouncycastle.pkix.syntax.jcaPEMKeyConverter.getKeyPairF
    import com.peknight.security.bouncycastle.pkix.syntax.jcaPEMWriter.writeObjectF
    import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
    import org.bouncycastle.openssl.PEMKeyPair

    import java.security.KeyPair
    val eitherT =
      for
        logger <- EitherT(Slf4jLogger.fromClass[IO](classOf[ACMEClientFlatSpec]).asError)
        given Logger[IO] = logger
        provider <- EitherT(BouncyCastleProvider[IO].asError)
        _ <- EitherT(Security.addProvider[IO](provider).asError)
        keyPair <- EitherT(secp256r1.generateKeyPair[IO]().asError)
        _ <- EitherT(writePEM[IO](Path("cert/test.key")) {writer =>
          for
            _ <- writer.writeObjectF[IO](keyPair.getPublic)
            _ <- writer.writeObjectF[IO](keyPair.getPrivate)
          yield
            ()
        })
        keyPair <- EitherT(readPEM[IO, KeyPair](Path("cert/test.key")) { list =>
          val et =
            for
              pemKeyPair <- Monad[[X] =>> Either[Error, X]]
                .tailRecM[(List[AnyRef], Option[PEMKeyPair], Option[SubjectPublicKeyInfo]), PEMKeyPair](
                  (list.toList, None, None)
                ) {
                  case ((pemKeyPair: PEMKeyPair) :: _, _, _) if Option(pemKeyPair.getPublicKeyInfo).isDefined =>
                    println("pem with pub")
                    pemKeyPair.asRight.asRight
                  case ((pemKeyPair: PEMKeyPair) :: _, _, Some(publicKeyInfo)) =>
                    println("pem & pub!")
                    new PEMKeyPair(publicKeyInfo, pemKeyPair.getPrivateKeyInfo).asRight.asRight
                  case ((publicKeyInfo: SubjectPublicKeyInfo) :: _, Some(pemKeyPair), _) =>
                    println("pub & pem!")
                    new PEMKeyPair(publicKeyInfo, pemKeyPair.getPrivateKeyInfo).asRight.asRight
                  case ((pemKeyPair: PEMKeyPair) :: tail, _, _) =>
                    println("pem no pub!")
                    (tail, Some(pemKeyPair), None).asLeft.asRight
                  case ((publicKeyInfo: SubjectPublicKeyInfo) :: tail, _, _) =>
                    println("pub no pem!")
                    (tail, None, Some(publicKeyInfo)).asLeft.asRight
                  case (head :: tail, pemKeyPairOption, publicKeyInfoOption) =>
                    println(s"${head.getClass}: $head")
                    (tail, pemKeyPairOption, publicKeyInfoOption).asLeft.asRight
                  case (Nil, _, _) => WrongClassTag[KeyPair](list.head).asLeft
                }.eLiftET[IO]
              keyPair <- EitherT(JcaPEMKeyConverter(provider = none).getKeyPairF[IO](pemKeyPair).asError)
            yield
              keyPair
          et.value
        })
      yield
        println(keyPair)
        ()
    eitherT.value.rethrow.asserting(_ => assert(true))
  }
end ACMEClientFlatSpec
