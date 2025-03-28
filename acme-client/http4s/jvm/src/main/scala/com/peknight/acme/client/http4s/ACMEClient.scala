package com.peknight.acme.client.http4s

import cats.data.{EitherT, NonEmptyList}
import cats.effect.*
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.parallel.*
import cats.{Id, Show}
import com.peknight.acme.account.{Account, AccountClaims}
import com.peknight.acme.authorization.{Authorization, AuthorizationStatus}
import com.peknight.acme.bouncycastle.pkcs.PKCS10CertificationRequest
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.challenge.{ChallengeClaims, ChallengeStatus}
import com.peknight.acme.client.api
import com.peknight.acme.client.error.*
import com.peknight.acme.client.jose.{signEmptyString, signJson}
import com.peknight.acme.directory.Directory
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.identifier.IdentifierType.dns
import com.peknight.acme.order.*
import com.peknight.cats.effect.ext.Clock
import com.peknight.cats.ext.syntax.eitherT.{eLiftET, lLiftET, rLiftET}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.syntax.encoder.asS
import com.peknight.codec.{Decoder, Encoder}
import com.peknight.commons.time.syntax.temporal.plus
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.error.syntax.either.label
import com.peknight.http.HttpResponse
import com.peknight.http.method.retry.syntax.eitherF.retry
import com.peknight.jose.jwk.{JsonWebKey, KeyId}
import com.peknight.jose.jws.JsonWebSignature
import com.peknight.logging.syntax.either.log
import com.peknight.logging.syntax.eitherF.log
import com.peknight.logging.syntax.eitherT.log
import com.peknight.validation.collection.list.either.one
import com.peknight.validation.std.either.{isTrue, typed}
import io.circe.Json
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scodec.bits.ByteVector

import java.security.cert.X509Certificate
import java.security.{KeyPair, PublicKey}
import java.util.Locale
import scala.concurrent.duration.*

class ACMEClient[F[_], Challenge <: com.peknight.acme.challenge.Challenge](
  directoryUri: Uri,
  directoryMaxAge: FiniteDuration,
  acmeApi: api.ACMEApi[F],
  nonceRef: Ref[F, Option[Base64UrlNoPad]],
  directoryRef: Ref[F, Option[HttpResponse[Directory]]]
)(using Async[F], Logger[F], Decoder[Id, Cursor[Json], Challenge]) extends api.ACMEClient[F, Challenge]:
  private given [X]: Show[X] = Show.fromToString[X]

  def directory: F[Either[Error, Directory]] =
    val eitherT =
      for
        directory <- EitherT(acmeApi.directory(directoryUri))
        now <- EitherT(Clock.realTimeInstant[F].asError)
        _ <- EitherT(directoryRef.update {
          case Some(HttpResponse(status, headers, body, None)) =>
            Some(HttpResponse(status, headers, body, Some(now.plus(directoryMaxAge))))
          case directoryR => directoryR
        }.asError)
      yield
        directory
    eitherT.value

  def nonce: F[Either[Error, Base64UrlNoPad]] =
    val eitherT =
      for
        nonceOption <- EitherT(nonceRef.getAndSet(None).asError)
        nonce <- nonceOption match
          case Some(nonce) => nonce.rLiftET
          case _ =>
            for
              directory <- EitherT(directory)
              nonce <- EitherT(acmeApi.newNonce(directory.newNonce))
            yield
              nonce
      yield
        nonce
    eitherT.value

  def newAccount(claims: AccountClaims, keyPair: KeyPair): F[Either[Error, (Account, Uri)]] =
    val eitherT =
      for
        directory <- EitherT(directory)
        response <- EitherT(postAsGet[AccountClaims, (Account, Uri)](directory.newAccount, claims, keyPair)(
          acmeApi.newAccount
        ))
      yield
        response
    eitherT.value

  def queryAccount(keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]] =
    postAsGet[Account](accountLocation, keyPair, accountLocation)(acmeApi.account)

  def newOrder(claims: OrderClaims, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, (Order, Uri)]] =
    val eitherT =
      for
        directory <- EitherT(directory)
        _ <- claims.autoRenewal.fold(().rLiftET[F, Error]) { autoRenewal => (claims.notBefore, claims.notAfter) match
          case (Some(_), Some(_)) => CanNotCombineWithAutoRenewal("notBefore&notAfter").lLiftET
          case (Some(_), None) => CanNotCombineWithAutoRenewal("notBefore").lLiftET
          case (None, Some(_)) => CanNotCombineWithAutoRenewal("notAfter").lLiftET
          case _ if !directory.meta.exists(_.autoRenewalEnabled) => AutoRenewalNotSupported.lLiftET
          case _ if autoRenewal.allowCertificateGet.isDefined && !directory.meta.exists(_.autoRenewalGetAllowed) =>
            AutoRenewalGetNotSupported.lLiftET
          case _ => ().rLiftET
        }
        _ <-
          if claims.replaces.isDefined && directory.renewalInfo.isEmpty then RenewalInfoNotSupported.lLiftET
          else ().rLiftET
        _ <- claims.profile.fold(().rLiftET[F, Error]) { profile =>
          if !directory.meta.exists(_.profileAllowed(profile)) then ProfileNotSupported(profile).lLiftET
          else ().rLiftET
        }
        hasAncestorDomain = claims.identifiers.filter(_.`type` === dns).exists(_.ancestorDomain.isDefined)
        _ <-
          if hasAncestorDomain && !directory.meta.flatMap(_.subdomainAuthAllowed).getOrElse(false) then
            AncestorDomainNotSupported.lLiftET[F, Unit]
          else ().rLiftET
        nonce <- EitherT(nonce)
        jws <- EitherT(signJson[F, OrderClaims](directory.newOrder, claims, keyPair, Some(nonce),
          Some(KeyId(accountLocation.toString))))
        response <- EitherT(acmeApi.newOrder(jws, directory.newOrder))
      yield
        response
    eitherT.value

  def queryOrder(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, HttpResponse[Order]]] =
    postAsGet[HttpResponse[Order]](orderLocation, keyPair, accountLocation)(acmeApi.order)

  def queryOrderRetry(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri)
                     (timeout: FiniteDuration = 1.minutes, interval: FiniteDuration = 3.seconds,
                      statusSet: Set[OrderStatus]): F[Either[Error, Order]] =
    queryOrder(orderLocation, keyPair, accountLocation)
      .log(name = "ACMEClient#queryOrder", param = Some(orderLocation))
      .retry(timeout = timeout.some, interval = interval.some)(
        _.map(_.body.status).exists(statusSet.contains)
      )((either, state, retry) => either.log(name = "ACMEClient#queryOrder#retry", param = (state, retry).some))
      .map(_.map(_.body))

  def finalizeOrder(finalizeUri: Uri, claims: FinalizeClaims, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Order]] =
    postAsGet[FinalizeClaims, Order](finalizeUri, claims, keyPair, Some(accountLocation))(acmeApi.finalizeOrder)

  def queryAuthorization(authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Authorization[Challenge]]] =
    postAsGet[Authorization[Challenge]](authorizationUri, keyPair, accountLocation)(acmeApi.authorization[Challenge])

  def queryChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, HttpResponse[Challenge]]] =
    postAsGet[HttpResponse[Challenge]](challengeUri, keyPair, accountLocation)(acmeApi.challenge[Challenge])

  def queryChallengeRetry(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri)
                         (timeout: FiniteDuration = 1.minutes, interval: FiniteDuration = 3.seconds)
  : F[Either[Error, Challenge]] =
    queryChallenge(challengeUri, keyPair, accountLocation)
      .log(name = "ACMEClient#queryChallenge", param = Some(challengeUri))
      .retry(timeout = timeout.some, interval = interval.some)(
        _.map(_.body.status).exists(Set(ChallengeStatus.valid, ChallengeStatus.invalid).contains)
      )((either, state, retry) => either.log(name = "ACMEClient#queryChallenge#retry", param = (state, retry).some))
      .map(_.map(_.body))

  def updateChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Challenge]] =
    postAsGet[ChallengeClaims, HttpResponse[Challenge]](challengeUri, ChallengeClaims(), keyPair, Some(accountLocation))(
      acmeApi.challenge[Challenge]
    ).map(_.map(_.body))

  def acceptChallenge[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, A](authorization: Authorization[Challenge], publicKey: PublicKey)
                                                                                     (ic: Authorization[Challenge] => Either[Error, (I, C)])
                                                                                     (prepare: (I, C, PublicKey) => F[Either[Error, Option[A]]])
  : F[Either[Error, Option[(I, C, Option[A])]]] =
    if authorization.status === AuthorizationStatus.valid then
      none[(I, C, Option[A])].asRight[Error].pure[F]
    else
      ic(authorization) match
        case Right((identifier, challenge)) =>
          if challenge.status === ChallengeStatus.valid then none[(I, C, Option[A])].asRight[Error].pure[F]
          else prepare(identifier, challenge, publicKey).map(_.map((identifier, challenge, _).some))
        case Left(error) => error.asLeft[Option[(I, C, Option[A])]].pure[F]
  end acceptChallenge

  def getDnsIdentifierAndChallenge(authorization: Authorization[Challenge]): Either[Error, (DNS, `dns-01`)] =
    for
      identifier <- typed[DNS](authorization.identifier).label("identifier")
      dnsChallenges: List[`dns-01`] = authorization.challenges.collect {
        case challenge: `dns-01` => challenge
      }
      challenge <- one(dnsChallenges).label("dnsChallenges")
    yield
      (identifier, challenge)

  def downloadCertificate(certificateUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, (NonEmptyList[X509Certificate], Option[List[Uri]])]] =
    postAsGet[(NonEmptyList[X509Certificate], Option[List[Uri]])](certificateUri, keyPair, accountLocation)(
      acmeApi.certificate
    )

  def fetchCertificates[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, A](
    identifiers: NonEmptyList[Identifier],
    accountKeyPair: F[Either[Error, KeyPair]],
    domainKeyPair: F[Either[Error, KeyPair]],
    sleepAfterPrepare: Duration = 2.minutes,
    queryChallengeTimeout: FiniteDuration = 1.minutes,
    queryChallengeInterval: FiniteDuration = 3.seconds,
    orderTimeout: FiniteDuration = 1.minutes,
    orderInterval: FiniteDuration = 3.seconds
  )(ic: Authorization[Challenge] => Either[Error, (I, C)]
  )(prepare: (I, C, PublicKey) => F[Either[Error, Option[A]]]
  )(clean: (I, C, Option[A]) => F[Either[Error, Unit]]): F[Either[Error, NonEmptyList[X509Certificate]]] =
    given Show[KeyPair] = Show.show(keyPair =>
      JsonWebKey.fromKeyPair(keyPair).map(_.asS[Id, Json].deepDropNullValues.noSpaces).getOrElse(keyPair.toString)
    )
    val eitherT =
      for
        accountKeyPair <- EitherT(accountKeyPair).log(name = "ACMEClient#accountKeyPair")
        accountClaims = AccountClaims(termsOfServiceAgreed = Some(true))
        (account, accountLocation) <- EitherT(newAccount(accountClaims, accountKeyPair))
          .log(name = "ACMEClient#newAccount", param = Some(accountClaims))
        orderClaims = OrderClaims(identifiers)
        (order, orderLocation) <- EitherT(newOrder(orderClaims, accountKeyPair, accountLocation))
          .log(name = "ACMEClient#newOrder", param = Some(orderClaims))
        authorizations <- order.authorizations.parTraverse { authorizationUri =>
          for
            authorization <- EitherT(queryAuthorization(authorizationUri, accountKeyPair, accountLocation))
              .log(name = "ACMEClient#queryAuthorization", param = Some(authorizationUri))
            authorization <- Resource.make[[X] =>> EitherT[F, Error, X], Option[(I, C, Option[A])]](
              EitherT(acceptChallenge[I, C, A](authorization, accountKeyPair.getPublic)(ic)(prepare))
                .log(name = "ACMEClient#acceptChallenge", param = Some(authorization))
            ){
              case Some((identifier, challenge, a)) =>
                EitherT(clean(identifier, challenge, a))
                  .log(name = "ACMEClient#clean", param = Some((identifier, challenge, a)))
              case None => EitherT.pure(())
            }.use {
              case Some((identifier, challenge, a)) =>
                for
                  _ <- EitherT(GenTemporal[F].sleep(sleepAfterPrepare).asError)
                  challenge <- EitherT(updateChallenge(challenge.url, accountKeyPair, accountLocation))
                    .log(name = "ACMEClient#updateChallenge", param = Some(challenge.url))
                  challenge <- EitherT(queryChallengeRetry(challenge.url, accountKeyPair, accountLocation)(
                    queryChallengeTimeout, queryChallengeInterval))
                  challenge <- isTrue(challenge.status === ChallengeStatus.valid,
                    ChallengeStatusNotValid(challenge.status).value(challenge)).eLiftET
                  authorization <- EitherT(queryAuthorization(authorizationUri, accountKeyPair, accountLocation))
                    .log(name = "ACMEClient#queryAuthorization", param = Some(authorizationUri))
                yield
                  authorization
              case None => authorization.rLiftET[F, Error]
            }
            _ <- isTrue(authorization.status === AuthorizationStatus.valid,
              AuthorizationStatusNotValid(authorization.status)).eLiftET
          yield
            authorization
        }
        order <- EitherT(queryOrderRetry(orderLocation, accountKeyPair, accountLocation)(orderTimeout, orderInterval,
          Set(OrderStatus.ready, OrderStatus.valid, OrderStatus.invalid)))
        _ <- isTrue(order.status === OrderStatus.ready, OrderStatusNotReady(order.status)).eLiftET
        generalNames <- order.toGeneralNames.eLiftET
        domainKeyPair <- EitherT(domainKeyPair).log(name = "ACMEClient#domainKeyPair")
        csr <- EitherT(PKCS10CertificationRequest.certificateSigningRequest[F](generalNames, domainKeyPair))
          .map(csr => Base64UrlNoPad.fromByteVector(ByteVector(csr.getEncoded)))
          .log(name = "ACMEClient#certificateSigningRequest", param = Some(generalNames))
        finalizeClaims = FinalizeClaims(csr)
        order <- EitherT(finalizeOrder(order.finalizeUri, finalizeClaims, accountKeyPair, accountLocation))
          .log(name = "ACMEClient#finalizeOrder", param = Some(order.finalizeUri))
        order <- EitherT(queryOrderRetry(orderLocation, accountKeyPair, accountLocation)(orderTimeout, orderInterval,
          Set(OrderStatus.valid, OrderStatus.invalid)))
        _ <- isTrue(order.status === OrderStatus.valid, OrderStatusNotValid(order.status)).eLiftET
        certificateUri <- order.starCertificate.orElse(order.certificate).toRight(OptionEmpty.label("certificateUri")).eLiftET
        (certificates, alternates) <- EitherT(downloadCertificate(certificateUri, accountKeyPair, accountLocation))
          .log(name = "ACMEClient#downloadCertificate", param = Some(certificateUri))
      yield
        certificates
    eitherT.value

  private def postAsGet[A, B](uri: Uri, payload: A, keyPair: KeyPair, accountLocation: Option[Uri] = None)
                             (f: (JsonWebSignature, Uri) => F[Either[Error, B]])
                             (using Encoder[Id, Json, A])
  : F[Either[Error, B]] =
    val eitherT =
      for
        nonce <- EitherT(nonce)
        jws <- EitherT(signJson[F, A](uri, payload, keyPair, Some(nonce),
          accountLocation.map(location => KeyId(location.toString))))
        response <- EitherT(f(jws, uri))
      yield
        response
    eitherT.value

  private def postAsGet[A](uri: Uri, keyPair: KeyPair, accountLocation: Uri)
                          (f: (JsonWebSignature, Uri) => F[Either[Error, A]]): F[Either[Error, A]] =
    val eitherT =
      for
        nonce <- EitherT(nonce)
        jws <- EitherT(signEmptyString[F](uri, keyPair, Some(nonce), Some(KeyId(accountLocation.toString))))
        response <- EitherT(f(jws, uri))
      yield
        response
    eitherT.value
end ACMEClient
object ACMEClient:
  def apply[F[_], Challenge <: com.peknight.acme.challenge.Challenge](client: Client[F], directoryUri: Uri,
                                                                      directoryMaxAge: FiniteDuration = 10.minutes)
                                                                     (dsl: Http4sClientDsl[F])
                                                                     (using Async[F], Decoder[Id, Cursor[Json], Challenge])
  : F[api.ACMEClient[F, Challenge]] =
    for
      locale <- Sync[F].blocking(Locale.getDefault)
      nonceRef <- Ref[F].of(none[Base64UrlNoPad])
      directoryRef <- Ref[F].of(none[HttpResponse[Directory]])
      logger <- Slf4jLogger.fromClass[F](ACMEClient.getClass)
      given Logger[F] = logger
      acmeApi = ACMEApi[F](locale, true, nonceRef, directoryRef)(client)(dsl)
    yield
      new ACMEClient[F, Challenge](directoryUri, directoryMaxAge, acmeApi, nonceRef, directoryRef)
end ACMEClient
