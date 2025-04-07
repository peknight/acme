package com.peknight.acme.client.http4s

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.{Id, Show}
import com.comcast.ip4s.port
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.client.cloudflare.DNSChallengeClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.acme.client.letsencrypt.uri
import com.peknight.acme.client.letsencrypt.uri.stagingDirectory
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.cloudflare.test.{pekToken, pekZoneId}
import com.peknight.codec.syntax.encoder.asS
import com.peknight.error.std.JavaThrowable
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.jose.jwk.JsonWebKey
import com.peknight.logging.syntax.eitherT.log
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.bouncycastle.openssl.{fetchKeyPair, fetchX509Certificates}
import com.peknight.security.cipher.RSA
import com.peknight.security.ecc.sec.secp256r1
import com.peknight.security.key.store.pkcs12
import com.peknight.security.syntax.keyStore.storePath
import fs2.io.file.Path
import fs2.io.net.Network
import io.circe.Json
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.http4s.*
import org.http4s.client.dsl
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger as MiddlewareLogger
import org.scalatest.flatspec.AsyncFlatSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.security.cert.X509Certificate
import java.security.{KeyPair, Provider}
import javax.security.auth.x500.X500Principal
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
                accountKeyPair <- EitherT(fetchKeyPair[IO](Path("cert/account.key"))(
                  secp256r1.generateKeyPair[IO](provider = Some(provider)).asError)).log(name = "accountKeyPair")
                domainKeyPair <- EitherT(fetchKeyPair[IO](Path("cert/domain.key"))(
                  RSA.keySizeGenerateKeyPair[IO](4096).asError)).log(name = "domainKeyPair")
                certificates <- EitherT(fetchX509Certificates[IO](Path("cert/domain-chain.crt"), provider = Some(provider)) {
                  val et =
                    for
                      identifiers <- NonEmptyList.of(
                        "*.peknight.com",
                        "*.server.peknight.com",
                        "*.ctrl.peknight.com",
                        "*.cdn.peknight.com"
                      ).traverse(domain => Identifier.dns(domain).eLiftET[IO])
                      certificates <- EitherT(acmeClient.fetchCertificate[DNS, `dns-01`, DNSRecordId](
                        identifiers,
                        accountKeyPair.asRight.pure,
                        domainKeyPair.asRight.pure
                      )(
                        acmeClient.getDnsIdentifierAndChallenge
                      )(
                        dnsChallengeClient.createDNSRecord
                      )(
                        dnsChallengeClient.cleanDNSRecord(_, _, _).map(_.as(()))
                      )).log(name = "fetchCertificates", param = Some(identifiers))
                    yield
                      certificates.certificates
                  et.value
                }).log(name = "certificates")
//                keyStore <- EitherT(pkcs12[IO]("", domainKeyPair.getPrivate, "", certificates,
//                  Some(provider)).asError).log(name = "keyStore")
                keyStore <- EitherT(buildKeyStore(domainKeyPair, certificates, None).asError)
//                _ <- EitherT(keyStore.storePath[IO](Path("cert/domain.p12"), "abcdef").asError).log(name = "storePath")
//                keyStore <- EitherT(pkcs12.loadPath[IO](Path("cert/domain.p12"), "abcdef", Some(provider)).asError)
//                  .log(name = "loadPath")
//                tlsContext <- EitherT(Network[IO].tlsContext.fromKeyStoreFile(Path("cert/domain-openssl.p12").toNioPath, "123456".toCharArray, "123456".toCharArray).asError)
                tlsContext <- EitherT(Network[IO].tlsContext.fromKeyStore(keyStore, "123456".toCharArray).asError)
                res <- EitherT(EmberServerBuilder.default[IO]
                  .withHostOption(None)
                  .withPort(port"8080")
                  .withTLS(tlsContext)
                  .withHttpApp(MiddlewareLogger.httpApp(true, false)(HttpRoutes.of[IO] {
                    case req => Ok("rua")
                  }.orNotFound))
                  .build.allocated
                  .asError)
                _ <- EitherT(IO.never.asError)
              yield
                ()
            eitherT.value
          }
      yield either
    run.asserting(either =>
      println(either.left.map(error => error.asInstanceOf[JavaThrowable[Throwable]].error.printStackTrace()))
      assert(either.isRight))
  }

  private def buildKeyStore(keyPair: KeyPair, chain: NonEmptyList[java.security.cert.Certificate], provider: Option[Provider | java.security.Provider] = None): IO[java.security.KeyStore] =
    val certChain = chain.toList.toArray
    given CanEqual[Any, Any] = CanEqual.derived
    for (i <- 0 until certChain.length - 1) {
      val issuerDN = certChain(i).asInstanceOf[X509Certificate].getIssuerX500Principal
      val subjectDN = certChain(i + 1).asInstanceOf[X509Certificate].getSubjectX500Principal
      println(s"[iss]: $issuerDN [sub]: $subjectDN")
      if (!(issuerDN == (subjectDN))) println("mie")
    }
    for
      keyStore <- pkcs12.getKeyStore[IO](provider)
      _ <- IO(keyStore.load(null, null))
      _ <- IO(keyStore.setKeyEntry("", keyPair.getPrivate, "123456".toCharArray, chain.toList.toArray))
    yield
      keyStore
end ACMEApiFlatSpec
