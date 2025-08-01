package com.peknight.acme.client.http4s

import cats.data.{EitherT, NonEmptyList}
import cats.effect.*
import cats.syntax.applicative.*
import cats.syntax.contravariant.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.parallel.*
import cats.{Id, Parallel, Show}
import com.peknight.acme.account.{Account, AccountClaims, AccountStatus, KeyChangeClaims}
import com.peknight.acme.authorization.{Authorization, AuthorizationClaims, AuthorizationStatus, PreAuthorizationClaims}
import com.peknight.acme.bouncycastle.pkcs.PKCS10CertificationRequest
import com.peknight.acme.certificate.RevokeClaims
import com.peknight.acme.challenge.{ChallengeClaims, ChallengeStatus}
import com.peknight.acme.client.api
import com.peknight.acme.client.api.ChallengeClient
import com.peknight.acme.client.error.*
import com.peknight.acme.client.jose.{signEmptyString, signJson}
import com.peknight.acme.context.ACMEContext
import com.peknight.acme.directory.Directory
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.IdentifierType.dns
import com.peknight.acme.order.*
import com.peknight.cats.effect.ext.Clock
import com.peknight.cats.ext.syntax.eitherT.{eLiftET, lLiftET, rLiftET}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.{Decoder, Encoder}
import com.peknight.commons.time.syntax.temporal.plus
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asET
import com.peknight.http.HttpResponse
import com.peknight.http.syntax.eitherF.retry
import com.peknight.jose.jwk.{JsonWebKey, KeyId}
import com.peknight.jose.jws.JsonWebSignature
import com.peknight.logging.syntax.either.log
import com.peknight.logging.syntax.eitherF.log
import com.peknight.logging.syntax.eitherT.log
import com.peknight.method.retry.{Retry, RetryState}
import com.peknight.security.certificate.revocation.list.ReasonCode
import com.peknight.security.certificate.showCertificate
import com.peknight.security.provider.Provider
import com.peknight.validation.std.either.isTrue
import io.circe.Json
import org.bouncycastle.asn1.x509.GeneralNames
import org.http4s.Uri
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scodec.bits.ByteVector

import java.security.cert.{Certificate, X509Certificate}
import java.security.{KeyPair, PublicKey, Provider as JProvider}
import java.util.Locale
import scala.concurrent.duration.*

class ACMEClient[F[_], Challenge <: com.peknight.acme.challenge.Challenge](
  directoryUri: Uri,
  directoryMaxAge: FiniteDuration,
  acmeApi: api.ACMEApi[F],
  nonceRef: Ref[F, Option[Base64UrlNoPad]],
  directoryRef: Ref[F, Option[HttpResponse[Directory]]]
)(using Async[F], Parallel[F], Logger[F], Decoder[Id, Cursor[Json], Challenge], Show[Challenge])
  extends api.ACMEClient[F, Challenge]:

  private given Show[RetryState] = Show.fromToString[RetryState]
  private given Show[Retry] = Show.fromToString[Retry]
  private given Show[KeyPair] = JsonWebKey.showKeyPair[KeyPair]
  private given Show[X509Certificate] = showCertificate

  def directory: F[Either[Error, Directory]] =
    val eitherT =
      for
        directory <- EitherT(acmeApi.directory(directoryUri))
        now <- Clock.realTimeInstant[F].asET
        _ <- directoryRef.update {
          case Some(HttpResponse(status, headers, body, None)) =>
            HttpResponse(status, headers, body, now.plus(directoryMaxAge).some).some
          case directoryR => directoryR
        }.asET
      yield
        directory
    eitherT.value

  def nonce: F[Either[Error, Base64UrlNoPad]] =
    val eitherT =
      for
        nonceOption <- nonceRef.getAndSet(None).asET
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

  def updateAccount(claims: AccountClaims, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]] =
    postAsGet[AccountClaims, Account](accountLocation, claims, keyPair, accountLocation.some)(acmeApi.account)

  def deactivateAccount(keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]] =
    updateAccount(AccountClaims(status = AccountStatus.deactivated.some), keyPair, accountLocation)

  def keyChange(newKeyPair: KeyPair, oldKeyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]] =
    val eitherT =
      for
        directory <- EitherT(directory)
        oldKey <- JsonWebKey.fromPublicKey(oldKeyPair.getPublic).eLiftET[F]
        claims = KeyChangeClaims(accountLocation, oldKey)
        innerJws <- EitherT(signJson[F, KeyChangeClaims](directory.keyChange, claims, newKeyPair))
        account <- EitherT(postAsGet[JsonWebSignature, Account](directory.keyChange, innerJws, oldKeyPair,
          accountLocation.some)(acmeApi.keyChange))
      yield
        account
    eitherT.value

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
        response <- EitherT(postAsGet[OrderClaims, (Order, Uri)](directory.newOrder, claims, keyPair,
          accountLocation.some)(acmeApi.newOrder))
      yield
        response
    eitherT.value

  def queryOrder(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, HttpResponse[Order]]] =
    postAsGet[HttpResponse[Order]](orderLocation, keyPair, accountLocation)(acmeApi.order)

  private def queryOrderRetry(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri)
                             (timeout: FiniteDuration = 1.minutes, interval: FiniteDuration = 3.seconds,
                              statusSet: Set[OrderStatus]): F[Either[Error, Order]] = {
    queryOrder(orderLocation, keyPair, accountLocation)
      .log("ACMEClient#queryOrder", orderLocation.some)
      .retry(timeout = timeout.some, interval = interval.some)(
        _.map(_.body.status).exists(statusSet.contains)
      )((either, state, retry) => either.log("ACMEClient#queryOrder#retry", (state, retry).some))
      .map(_.map(_.body))
  }

  def finalizeOrder(finalizeUri: Uri, claims: FinalizeClaims, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Order]] =
    postAsGet[FinalizeClaims, Order](finalizeUri, claims, keyPair, accountLocation.some)(acmeApi.finalizeOrder)

  def queryAuthorization(authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Authorization[Challenge]]] =
    postAsGet[Authorization[Challenge]](authorizationUri, keyPair, accountLocation)(acmeApi.authorization[Challenge])

  def deactivateAuthorization(authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Authorization[Challenge]]] =
    postAsGet[AuthorizationClaims, Authorization[Challenge]](authorizationUri,
      AuthorizationClaims(status = AuthorizationStatus.deactivated.some), keyPair, accountLocation.some)(
      acmeApi.authorization[Challenge]
    )

  def preAuthorization(claims: PreAuthorizationClaims, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, (Authorization[Challenge], Uri)]] =
    val eitherT =
      for
        directory <- EitherT(directory)
        newAuthorization <- directory.newAuthorization.toRight(OptionEmpty.label("newAuthorization")).eLiftET[F]
        authorization <- EitherT(postAsGet[PreAuthorizationClaims, (Authorization[Challenge], Uri)](newAuthorization,
          claims, keyPair, accountLocation.some)(acmeApi.newAuthorization[Challenge]))
      yield
        authorization
    eitherT.value

  def queryChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, HttpResponse[Challenge]]] =
    postAsGet[HttpResponse[Challenge]](challengeUri, keyPair, accountLocation)(acmeApi.challenge[Challenge])

  private def queryChallengeRetry(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri)
                                 (timeout: FiniteDuration = 1.minutes, interval: FiniteDuration = 3.seconds)
  : F[Either[Error, Challenge]] =
    queryChallenge(challengeUri, keyPair, accountLocation)
      .log("ACMEClient#queryChallenge", challengeUri.some)
      .retry(timeout = timeout.some, interval = interval.some)(
        _.map(_.body.status).exists(Set(ChallengeStatus.valid, ChallengeStatus.invalid).contains)
      )((either, state, retry) => either.log("ACMEClient#queryChallenge#retry", (state, retry).some))
      .map(_.map(_.body))

  def updateChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Challenge]] =
    postAsGet[ChallengeClaims, HttpResponse[Challenge]](challengeUri, ChallengeClaims(), keyPair, accountLocation.some)(
      acmeApi.challenge[Challenge]
    ).map(_.map(_.body))

  def acceptChallenge[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, Record](
    authorization: Authorization[Challenge], publicKey: PublicKey
  )(using challengeClient: ChallengeClient[F, Challenge, I, C, Record])
  : F[Either[Error, Option[(I, C, Option[Record])]]] =
    if authorization.status === AuthorizationStatus.valid then
      none[(I, C, Option[Record])].asRight[Error].pure[F]
    else
      challengeClient.getIdentifierAndChallenge(authorization) match
        case Right((identifier, challenge)) =>
          if challenge.status === ChallengeStatus.valid then none[(I, C, Option[Record])].asRight[Error].pure[F]
          else challengeClient.prepare(identifier, challenge, publicKey).map(_.map((identifier, challenge, _).some))
        case Left(error) => error.asLeft[Option[(I, C, Option[Record])]].pure[F]
  end acceptChallenge

  def downloadCertificate(certificateUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, (NonEmptyList[X509Certificate], Option[List[Uri]])]] =
    postAsGet[(NonEmptyList[X509Certificate], Option[List[Uri]])](certificateUri, keyPair, accountLocation)(
      acmeApi.certificate
    )

  def revokeCertificate(certificate: Certificate, keyPair: KeyPair, accountLocation: Uri,
                        reason: Option[ReasonCode] = None): F[Either[Error, Unit]] =
    val eitherT =
      for
        directory <- EitherT(directory)
        claims = RevokeClaims(Base64UrlNoPad.fromByteVector(ByteVector(certificate.getEncoded)), reason)
        response <- EitherT(postAsGet[RevokeClaims, Unit](directory.revokeCertificate, claims,
          keyPair, accountLocation.some)(acmeApi.revokeCertificate))
      yield
        response
    eitherT.value

  def fetchCertificate[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, Record](
    identifiers: NonEmptyList[Identifier],
    accountKeyPair: F[Either[Error, KeyPair]],
    domainKeyPair: F[Either[Error, KeyPair]],
    sleepAfterPrepare: FiniteDuration = 2.minutes,
    queryChallengeTimeout: FiniteDuration = 1.minutes,
    queryChallengeInterval: FiniteDuration = 3.seconds,
    queryOrderTimeout: FiniteDuration = 1.minutes,
    queryOrderInterval: FiniteDuration = 3.seconds,
    provider: Option[Provider | JProvider] = None
  )(using challengeClient: ChallengeClient[F, Challenge, I, C, Record])
  : F[Either[Error, ACMEContext[Challenge]]] =
    given Show[I] = Identifier.showIdentifier.contramap[I](identity)
    given Show[C] = Show[Challenge].contramap[C](_.asInstanceOf)
    given Show[Record] = Show.fromToString[Record]
    given Show[GeneralNames] = Show.show(_.getNames.mkString("GeneralNames(", ",", ")"))
    val eitherT =
      for
        accountKeyPair <- EitherT(accountKeyPair).log[Unit]("ACMEClient#accountKeyPair")
        accountClaims = AccountClaims(termsOfServiceAgreed = true.some)
        (account, accountLocation) <- EitherT(newAccount(accountClaims, accountKeyPair))
          .log("ACMEClient#newAccount", accountClaims.some)
        orderClaims = OrderClaims(identifiers)
        (order, orderLocation) <- EitherT(newOrder(orderClaims, accountKeyPair, accountLocation))
          .log("ACMEClient#newOrder", orderClaims.some)
        authorizations <- order.authorizations.parTraverse { authorizationUri =>
          for
            authorization <- EitherT(queryAuthorization(authorizationUri, accountKeyPair, accountLocation))
              .log("ACMEClient#queryAuthorization", authorizationUri.some)
            authorization <- Resource.make[[X] =>> EitherT[F, Error, X], Option[(I, C, Option[Record])]] {
              EitherT(acceptChallenge[I, C, Record](authorization, accountKeyPair.getPublic))
                .log("ACMEClient#acceptChallenge", authorization.some)
            }{
              case Some((identifier, challenge, a)) =>
                EitherT(challengeClient.clean(identifier, challenge, a))
                  .log("ACMEClient#clean", (identifier, challenge, a).some)
              case None => EitherT.pure(())
            }.use {
              case Some((identifier, challenge, a)) =>
                for
                  _ <- GenTemporal[F].sleep(sleepAfterPrepare).asET
                  challenge <- EitherT(updateChallenge(challenge.url, accountKeyPair, accountLocation))
                    .log("ACMEClient#updateChallenge", challenge.url.some)
                  challenge <-
                    if Set(ChallengeStatus.valid, ChallengeStatus.invalid).contains(challenge.status) then
                      challenge.rLiftET
                    else
                      for
                        _ <- GenTemporal[F].sleep(queryChallengeInterval).asET
                        challenge <- EitherT(queryChallengeRetry(challenge.url, accountKeyPair, accountLocation)(
                          queryChallengeTimeout, queryChallengeInterval))
                      yield
                        challenge
                  challenge <- isTrue(challenge.status === ChallengeStatus.valid,
                    ChallengeStatusNotValid(challenge.status).value(challenge)).eLiftET
                  authorization <- EitherT(queryAuthorization(authorizationUri, accountKeyPair, accountLocation))
                    .log("ACMEClient#queryAuthorization", authorizationUri.some)
                yield
                  authorization
              case None => authorization.rLiftET[F, Error]
            }
            _ <- isTrue(authorization.status === AuthorizationStatus.valid,
              AuthorizationStatusNotValid(authorization.status)).eLiftET
          yield
            authorization
        }
        order <- EitherT(queryOrderRetry(orderLocation, accountKeyPair, accountLocation)(queryOrderTimeout,
          queryOrderInterval, Set(OrderStatus.ready, OrderStatus.valid, OrderStatus.invalid)))
        _ <- isTrue(order.status === OrderStatus.ready, OrderStatusNotReady(order.status)).eLiftET
        generalNames <- order.toGeneralNames.eLiftET
        domainKeyPair <- EitherT(domainKeyPair).log[Unit]("ACMEClient#domainKeyPair")
        csr <- EitherT(PKCS10CertificationRequest.certificateSigningRequest[F](generalNames, domainKeyPair, provider))
          .map(csr => Base64UrlNoPad.fromByteVector(ByteVector(csr.getEncoded)))
          .log("ACMEClient#certificateSigningRequest", generalNames.some)
        finalizeClaims = FinalizeClaims(csr)
        order <- EitherT(finalizeOrder(order.finalizeUri, finalizeClaims, accountKeyPair, accountLocation))
          .log("ACMEClient#finalizeOrder", order.finalizeUri.some)
        order <-
          if Set(OrderStatus.valid, OrderStatus.invalid).contains(order.status) then order.rLiftET
          else
            for
              _ <- GenTemporal[F].sleep(queryOrderInterval).asET
              order <- EitherT(queryOrderRetry(orderLocation, accountKeyPair, accountLocation)(queryOrderTimeout,
                queryOrderInterval, Set(OrderStatus.valid, OrderStatus.invalid)))
            yield
              order
        _ <- isTrue(order.status === OrderStatus.valid, OrderStatusNotValid(order.status)).eLiftET
        certificateUri <- order.starCertificate.orElse(order.certificate).toRight(OptionEmpty.label("certificateUri"))
          .eLiftET
        (certificates, alternates) <- EitherT(downloadCertificate(certificateUri, accountKeyPair, accountLocation))
          .log("ACMEClient#downloadCertificate", certificateUri.some)
      yield
        ACMEContext(accountKeyPair, domainKeyPair, certificates, account, accountLocation, order, orderLocation,
          authorizations, alternates)
    eitherT.log("ACMEClient#fetchCertificate", identifiers.some).value

  private def postAsGet[A, B](uri: Uri, payload: A, keyPair: KeyPair, accountLocation: Option[Uri] = None)
                             (f: (JsonWebSignature, Uri) => F[Either[Error, B]])
                             (using Encoder[Id, Json, A])
  : F[Either[Error, B]] =
    val eitherT =
      for
        nonce <- EitherT(nonce)
        jws <- EitherT(signJson[F, A](uri, payload, keyPair, nonce.some,
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
        jws <- EitherT(signEmptyString[F](uri, keyPair, nonce.some, KeyId(accountLocation.toString).some))
        response <- EitherT(f(jws, uri))
      yield
        response
    eitherT.value
end ACMEClient
object ACMEClient:
  def apply[F[_], Challenge <: com.peknight.acme.challenge.Challenge](directoryUri: Uri,
                                                                      directoryMaxAge: FiniteDuration = 10.minutes,
                                                                      locale: Option[Locale] = None,
                                                                      compression: Boolean = true
                                                                     )
                                                                     (using Client[F], Async[F], Parallel[F],
                                                                      Decoder[Id, Cursor[Json], Challenge],
                                                                      Show[Challenge])
  : F[ACMEClient[F, Challenge]] =
    for
      locale <- locale.fold(Sync[F].blocking(Locale.getDefault))(_.pure[F])
      nonceRef <- Ref[F].of(none[Base64UrlNoPad])
      directoryRef <- Ref[F].of(none[HttpResponse[Directory]])
      logger <- Slf4jLogger.fromClass[F](ACMEClient.getClass)
      given Logger[F] = logger
      acmeApi = ACMEApi[F](locale, compression, nonceRef, directoryRef)
    yield
      new ACMEClient[F, Challenge](directoryUri, directoryMaxAge, acmeApi, nonceRef, directoryRef)
end ACMEClient
