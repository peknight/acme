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
import com.peknight.cloudflare.zone.codec.instances.config.cloudflareZoneConfig.given
import com.peknight.cloudflare.zone.config.CloudflareZoneConfig
import com.peknight.codec.Decoder
import com.peknight.codec.reader.Key
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.logging.syntax.eitherT.log
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.bouncycastle.openssl.{fetchKeyPair, fetchX509CertificatesAndKeyPair}
import com.peknight.security.cipher.RSA
import com.peknight.security.ecc.sec.secp384r1
import fs2.io.file.Path
import org.http4s.client.Client
import org.http4s.dsl.io.Path as _
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AsyncFlatSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

class ACMEClientFlatSpec extends AsyncFlatSpec with AsyncIOSpec:

  "ACME Client" should "succeed" in {
    val run =
      for
        logger <- Slf4jLogger.fromClass[IO](classOf[ACMEClientFlatSpec])
        given Logger[IO] = logger
        provider <- BouncyCastleProvider[IO]
        _ <- Security.addProvider[IO](provider)
        either <- EmberClientBuilder.default[IO].withLogger(logger).withTimeout(45.seconds).build
          .use { client =>
            given Client[IO] = client
            val eitherT =
              for
                stagingDirectory <- resolve(acmeStaging).eLiftET[IO]
                acmeClient <- EitherT(ACMEClient[IO, Challenge](stagingDirectory).asError)
                // 设置环境变量CLOUDFLARE_TOKEN及CLOUDFLARE_ZONE_ID
                config <- EitherT(Decoder.load[IO, CloudflareZoneConfig](Key("cloudflare")))
                given DNSRecordApi[IO] = DNSRecordApi[IO](config.token)
                dnsChallengeClient <- EitherT(CloudflareDNSChallengeClient[IO, Challenge](config.zoneId).asError)
                given CloudflareDNSChallengeClient[IO, Challenge] = dnsChallengeClient
                accountKeyPair <- EitherT(fetchKeyPair[IO](Path("cert/account.key"))(
                  RSA.keySizeGenerateKeyPair[IO](4096, provider = provider.some).asError))
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
                        secp384r1.generateKeyPair[IO](provider = provider.some).asError,
                        provider = provider.some
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

//  "ACME Client Http4s Server" should "succeed" in {
//    val run =
//      for
//        logger <- Slf4jLogger.fromClass[IO](classOf[ACMEClientFlatSpec])
//        given Logger[IO] = logger
//        provider <- BouncyCastleProvider[IO]
//        _ <- Security.addProvider[IO](provider)
//        either <- EmberClientBuilder.default[IO].withLogger(logger).withTimeout(10.seconds).build
//          .use { client =>
//            given Client[IO] = client
//            val eitherT =
//              for
//                stagingDirectory <- resolve(acmeStaging).eLiftET[IO]
//                acmeClient <- EitherT(ACMEClient[IO, Challenge](stagingDirectory).asError)
//                given DNSRecordApi[IO] = DNSRecordApi[IO](pekToken)
//                dnsChallengeClient <- EitherT(CloudflareDNSChallengeClient[IO, Challenge](pekZoneId).asError)
//                given CloudflareDNSChallengeClient[IO, Challenge] = dnsChallengeClient
//                accountKeyPair <- EitherT(fetchKeyPair[IO](Path("cert/account.key"))(
//                  RSA.keySizeGenerateKeyPair[IO](4096, provider = provider.some).asError))
//                identifiers <- NonEmptyList.of(
//                  "*.peknight.com",
//                ).traverse(domain => Identifier.dns(domain).eLiftET[IO])
//                context <- EitherT(acmeClient.fetchCertificate[DNS, `dns-01`, DNSRecordId](
//                  identifiers,
//                  accountKeyPair.asRight.pure,
//                  secp384r1.generateKeyPair[IO](provider = provider.some).asError,
//                  provider = provider.some
//                ))
//                given CanEqual[Method, Method] = CanEqual.derived
//                given CanEqual[org.http4s.dsl.io.Path, org.http4s.dsl.io.Path] = CanEqual.derived
//                httpApp = HttpRoutes.of[IO] { case req => Ok("Hello, world!") }.orNotFound
//                keyStore1 <- EitherT(pkcs12[IO]("", context.domainKeyPair.getPrivate, "", context.certificates,
//                  none).asError)
//                tlsContext1 <- EitherT(Network.forAsync[IO].tlsContext.fromKeyStore(keyStore1, "".toCharArray).asError)
//                _ <- EitherT(writeX509CertificatesAndKeyPair[IO](Path("cert/domain.crt"), Path("cert/domain.key"))(
//                  context.certificates, context.domainKeyPair))
//                opt <- EitherT(readX509CertificatesAndKeyPair[IO](Path("cert/domain.crt"), Path("cert/domain.key"),
//                  none, provider.some))
//                (certificates, domainKeyPair) <- opt.toRight(OptionEmpty.label("x509CertificatesAndKeyPair")).eLiftET[IO]
//                keyStore2 <- EitherT(pkcs12[IO]("", domainKeyPair.getPrivate, "", certificates,
//                  none).asError)
//                tlsContext2 <- EitherT(Network.forAsync[IO].tlsContext.fromKeyStore(keyStore2, "".toCharArray).asError)
//                _ <- EitherT(EmberServerBuilder.default[IO].withLogger(logger)
//                  .withHostOption(none)
//                  .withPort(port"8443")
//                  .withTLS(tlsContext1)
//                  .withHttpWebSocketApp(_ => MiddlewareLogger.httpApp[IO](true, true)(httpApp))
//                  .build
//                  .allocated
//                  .asError)
//                _ <- EitherT(EmberServerBuilder.default[IO].withLogger(logger)
//                  .withHostOption(none)
//                  .withPort(port"8444")
//                  .withTLS(tlsContext2)
//                  .withHttpWebSocketApp(_ => MiddlewareLogger.httpApp[IO](true, true)(httpApp))
//                  .build
//                  .allocated
//                  .asError)
//                _ <- EitherT(IO.never.asError)
//              yield
//                ()
//            eitherT.log[Unit]("ACMEClientHttp4sServer#run").value
//          }
//      yield either
//    run.asserting(either => assert(either.isRight))
//  }
end ACMEClientFlatSpec
