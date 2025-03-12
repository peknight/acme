package com.peknight.acme.client.http4s

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.parallel.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import com.peknight.acme.account.AccountClaims
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.client.cloudflare.DNSChallengeClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.acme.client.letsencrypt.uri
import com.peknight.acme.client.letsencrypt.uri.stagingDirectory
import com.peknight.acme.error.ACMEError
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.order.OrderClaims
import com.peknight.cats.ext.syntax.eitherT.{eLiftET, rLiftET}
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.cloudflare.test.{PekToken, PekZone}
import com.peknight.error.Error
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
                acmeClient <- EitherT(ACMEClient[IO, Challenge](client, stagingDirectory)(dsl.io).asError)
                given DNSRecordApi[IO] = DNSRecordApi[IO](PekToken.token)(client)(dsl.io)
                dnsChallengeClient <- EitherT(DNSChallengeClient[IO](PekZone.zoneId).asError)
                userKeyPair <- EitherT(secp256r1.generateKeyPair[IO](provider = Some(provider)).asError)
                account <- EitherT(acmeClient.newAccount(AccountClaims(termsOfServiceAgreed = Some(true)), userKeyPair))
                _ <- EitherT(info"account: $account".asError)
                accountLocation = account.location
                identifiers <- NonEmptyList.of(
                  "*.peknight.com",
                  "*.server.peknight.com",
                  "*.ctrl.peknight.com",
                  "*.cdn.peknight.com"
                ).traverse(domain => Identifier.dns(domain).eLiftET[IO])
                order <- EitherT(acmeClient.newOrder(OrderClaims(identifiers), userKeyPair,
                  accountLocation))
                orderLocation = order.location
                _ <- EitherT(info"order: $order".asError)
                authorizations <- order.body.authorizations.parTraverse { authorizationUri =>
                  for
                    authorization <- EitherT(acmeClient.authorization(authorizationUri, userKeyPair, accountLocation))
                    _ <- EitherT(info"authorization: $authorization".asError)
                    opt <- EitherT(acmeClient.challenge[DNS, `dns-01`, DNSRecordId](authorization)(
                      acmeClient.getDnsIdentifierAndChallenge(authorization)
                    )(dnsChallengeClient.createDNSRecord(_, _, userKeyPair.getPublic)))
                    _ <- EitherT(IO.sleep(10.seconds).asError)
                    challenge <- opt match
                      case Some((identifier, challenge, dnsRecordId)) =>
                        for
                          c <- EitherT(acmeClient.respondToChallenge(challenge.url, userKeyPair, accountLocation)).map(_.some)
                          _ <- EitherT(info"challenge: $c".asError)
                          _ <- EitherT(IO.sleep(10.seconds).asError)
                          c <- EitherT(acmeClient.challenge(challenge.url, userKeyPair, accountLocation)).map(_.some)
                          _ <- EitherT(info"challenge: $c".asError)
                        yield
                          c
                      case None => none[Challenge].rLiftET[IO, Error]
                  yield
                    authorization
                }
                domainKeyPair <- EitherT(RSA.keySizeGenerateKeyPair[IO](4096).asError)
                _ <- EitherT(IO.sleep(10.seconds).asError)
                account <- EitherT(acmeClient.account(userKeyPair, accountLocation))
                _ <- EitherT(info"account: $account".asError)
                order <- EitherT(acmeClient.order(orderLocation, userKeyPair, accountLocation))
                _ <- EitherT(info"order: $order".asError)
              yield
                account
            eitherT.value
          }
      yield either
    run.asserting(either => assert(either.isRight))
  }
end ACMEApiFlatSpec
