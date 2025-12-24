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
import com.peknight.cats.syntax.eitherT.eLiftET
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.cloudflare.zone.config.CloudflareZoneConfig
import com.peknight.codec.Decoder
import com.peknight.codec.reader.Key
import com.peknight.error.syntax.applicativeError.{asET, asError}
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
                acmeClient <- ACMEClient[IO, Challenge](stagingDirectory).asET
                // 设置环境变量CLOUDFLARE_TOKEN及CLOUDFLARE_ZONE_ID
                config <- EitherT(Decoder.load[IO, CloudflareZoneConfig](Key("cloudflare")))
                given DNSRecordApi[IO] = DNSRecordApi[IO](config.token)
                dnsChallengeClient <- CloudflareDNSChallengeClient[IO, Challenge](config.zoneId).asET
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

end ACMEClientFlatSpec
