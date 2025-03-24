package com.peknight.acme.client.http4s

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.option.*
import cats.syntax.parallel.*
import cats.{Id, Show}
import com.peknight.acme.account.AccountClaims
import com.peknight.acme.authorization.AuthorizationStatus
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.challenge.ChallengeStatus
import com.peknight.acme.client.cloudflare.DNSChallengeClient
import com.peknight.acme.client.error.{AuthorizationStatusNotValid, ChallengeStatusNotValid, OrderStatusNotReady}
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.acme.client.letsencrypt.uri
import com.peknight.acme.client.letsencrypt.uri.stagingDirectory
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.order.{OrderClaims, OrderStatus}
import com.peknight.cats.ext.syntax.eitherT.{eLiftET, rLiftET}
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.cloudflare.test.{PekToken, PekZone}
import com.peknight.codec.Encoder
import com.peknight.codec.syntax.encoder.asS
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.http.HttpResponse
import com.peknight.http.method.retry.syntax.eitherT.retry
import com.peknight.jose.jwk.JsonWebKey
import com.peknight.logging.syntax.either.log
import com.peknight.logging.syntax.eitherT.log
import com.peknight.method.retry.Retry
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.cipher.RSA
import com.peknight.security.ecc.sec.secp256r1
import com.peknight.validation.std.either.isTrue
import io.circe.Json
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
    def showJson[A](using Encoder[Id, Json, A]): Show[A] = Show.show(a => a.asS[Id, Json].deepDropNullValues.noSpaces)
    given [A]: Show[A] = Show.fromToString[A]
    given Show[KeyPair] = Show.show(keyPair =>
      JsonWebKey.fromKeyPair(keyPair).map(_.asS[Id, Json].deepDropNullValues.noSpaces).getOrElse(keyPair.toString)
    )
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
                  .log(name = "ACMEClient#generateUserKeyPair")
                accountClaims = AccountClaims(termsOfServiceAgreed = Some(true))
                account <- EitherT(acmeClient.newAccount(accountClaims, userKeyPair))
                  .log(name = "ACMEClient#newAccount", param = Some(accountClaims))
                accountLocation = account.location
                identifiers <- NonEmptyList.of(
                  "*.peknight.com",
                  "*.server.peknight.com",
                  "*.ctrl.peknight.com",
                  "*.cdn.peknight.com"
                ).traverse(domain => Identifier.dns(domain).eLiftET[IO])
                orderClaims = OrderClaims(identifiers)
                order <- EitherT(acmeClient.newOrder(orderClaims, userKeyPair, accountLocation))
                  .log(name = "ACMEClient#newOrder", param = Some(orderClaims))
                orderLocation = order.location
                authorizations <- order.body.authorizations.parTraverse { authorizationUri =>
                  for
                    authorization <- EitherT(acmeClient.authorization(authorizationUri, userKeyPair, accountLocation))
                      .log(name = "ACMEClient#authorization", param = Some(authorizationUri))
                    opt <- EitherT(acmeClient.challenge[DNS, `dns-01`, DNSRecordId](authorization)(
                      acmeClient.getDnsIdentifierAndChallenge(authorization)
                    )(dnsChallengeClient.createDNSRecord(_, _, userKeyPair.getPublic)))
                      .log(name = "ACMEClient#challenge", param = Some(authorization))
                    _ <- EitherT(IO.sleep(2.minutes).asError)
                    authorization <- opt match
                      case Some((identifier, challenge, dnsRecordId)) =>
                        for
                          c <- EitherT(acmeClient.updateChallenge(challenge.url, userKeyPair, accountLocation))
                            .log(name = "ACMEClient#updateChallenge", param = Some(challenge.url))
                            .map(_.some)
                          _ <- EitherT(IO.sleep(5.seconds).asError)
                          c <- EitherT(acmeClient.queryChallenge(challenge.url, userKeyPair, accountLocation))
                            .log(name = "ACMEClient#queryChallenge", param = Some(challenge.url))
                            .retry(timeout = 1.minutes.some, interval = 3.seconds.some)(
                              _.map(_.body.status).exists(Set(ChallengeStatus.valid, ChallengeStatus.invalid).contains)
                            )((either, state, retry) => either.log(name = "ACMEClient#queryChallenge#retry",
                              param = (state, retry).some))
                          c <- isTrue(c.body.status === ChallengeStatus.valid,
                            c.body.error.getOrElse(ChallengeStatusNotValid(c.body.status))).eLiftET
                          authorization <- EitherT(acmeClient.authorization(authorizationUri, userKeyPair, accountLocation))
                            .log(name = "ACMEClient#authorization", param = Some(authorizationUri))
                        yield
                          authorization
                      case None => authorization.rLiftET[IO, Error]
                    _ <- isTrue(authorization.status === AuthorizationStatus.valid,
                      AuthorizationStatusNotValid(authorization.status)).eLiftET
                  yield
                    authorization
                }
                order <- EitherT(acmeClient.order(orderLocation, userKeyPair, accountLocation))
                  .log(name = "ACMEClient#order", param = Some(orderLocation))
                  .retry(timeout = 1.minutes.some, interval = 3.seconds.some)(
                    _.map(_.body.status).exists(Set(OrderStatus.ready, OrderStatus.valid, OrderStatus.invalid).contains)
                  )((either, state, retry) => either.log(name = "ACMEClient#order#retry",
                    param = (state, retry).some))
                _ <- isTrue(order.body.status === OrderStatus.ready, OrderStatusNotReady(order.body.status)).eLiftET
                domainKeyPair <- EitherT(RSA.keySizeGenerateKeyPair[IO](4096).asError)
                _ <- EitherT(acmeClient.account(userKeyPair, accountLocation)).log(name = "ACMEClient#account")
              yield
                account
            eitherT.value
          }
      yield either
    run.asserting(either => assert(either.isRight))
  }
end ACMEApiFlatSpec
