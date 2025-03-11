package com.peknight.acme.client.http4s

import cats.data.EitherT
import cats.effect.{Async, Ref, Sync}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.{Id, Show}
import com.peknight.acme.account.{AccountClaims, NewAccountHttpResponse, NewAccountResponse}
import com.peknight.acme.authorization.{Authorization, AuthorizationStatus}
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.challenge.{ChallengeClaims, ChallengeStatus}
import com.peknight.acme.client.api
import com.peknight.acme.client.api.DNSChallengeClient
import com.peknight.acme.client.error.*
import com.peknight.acme.client.jose.{signEmptyString, signJson}
import com.peknight.acme.directory.Directory
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.identifier.IdentifierType.dns
import com.peknight.acme.order.{NewOrderHttpResponse, Order, OrderClaims}
import com.peknight.cats.effect.ext.Clock
import com.peknight.cats.ext.syntax.eitherT.{eLiftET, lLiftET, rLiftET}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.{Decoder, Encoder}
import com.peknight.commons.time.syntax.temporal.plus
import com.peknight.error.Error
import com.peknight.error.collection.CollectionEmpty
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.error.syntax.either.label
import com.peknight.http.HttpResponse
import com.peknight.jose.jwk.KeyId
import com.peknight.jose.jws.JsonWebSignature
import com.peknight.validation.collection.list.either.one
import com.peknight.validation.std.either.typed
import io.circe.Json
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.security.{KeyPair, PublicKey}
import java.util.Locale
import scala.concurrent.duration.*

class ACMEClient[F[_], Challenge <: com.peknight.acme.challenge.Challenge](
  directoryUri: Uri,
  directoryMaxAge: FiniteDuration,
  acmeApi: api.ACMEApi[F],
  nonceRef: Ref[F, Option[Base64UrlNoPad]],
  directoryRef: Ref[F, Option[HttpResponse[Directory]]]
)(using Sync[F], Decoder[Id, Cursor[Json], Challenge]) extends api.ACMEClient[F, Challenge]:
  def directory: F[Either[Error, Directory]] =
    val eitherT =
      for
        directory <- EitherT(acmeApi.directory(directoryUri))
        now <- EitherT(Clock.realTimeInstant[F].asError)
        _ <- EitherT(directoryRef.update {
          case Some(HttpResponse(headers, body, None)) =>
            Some(HttpResponse(headers, body, Some(now.plus(directoryMaxAge))))
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

  def newAccount(claims: AccountClaims, keyPair: KeyPair): F[Either[Error, NewAccountHttpResponse]] =
    val eitherT =
      for
        directory <- EitherT(directory)
        response <- EitherT(postAsGet[AccountClaims, NewAccountHttpResponse](directory.newAccount, claims, keyPair)(
          acmeApi.newAccount
        ))
      yield
        response
    eitherT.value

  def account(keyPair: KeyPair, accountLocation: Uri): F[Either[Error, NewAccountResponse]] =
    postAsGet[NewAccountResponse](accountLocation, keyPair, accountLocation)(acmeApi.account)

  def newOrder(claims: OrderClaims, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, NewOrderHttpResponse]] =
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

  def order(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Order]] =
    postAsGet[Order](orderLocation, keyPair, accountLocation)(acmeApi.order)

  def authorization(authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Authorization[Challenge]]] =
    postAsGet[Authorization[Challenge]](authorizationUri, keyPair, accountLocation)(acmeApi.authorization[Challenge])

  def challenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Challenge]] =
    postAsGet[Challenge](challengeUri, keyPair, accountLocation)(acmeApi.challenge[Challenge])

  def respondToChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Challenge]] =
    postAsGet[ChallengeClaims, Challenge](challengeUri, ChallengeClaims(), keyPair, Some(accountLocation))(
      acmeApi.challenge[Challenge]
    )

  def challenge[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, A](authorization: Authorization[Challenge])
                                                                               (ci: => Either[Error, (I, C)])
                                                                               (f: (I, C) => F[Either[Error, Option[A]]])
  : F[Either[Error, Option[(I, C, Option[A])]]] =
    if authorization.status === AuthorizationStatus.valid then
      none[(I, C, Option[A])].asRight[Error].pure[F]
    else
      ci match
        case Right((identifier, challenge)) =>
          if challenge.status === ChallengeStatus.valid then none[(I, C, Option[A])].asRight[Error].pure[F]
          else f(identifier, challenge).map(_.map((identifier, challenge, _).some))
        case Left(error) => error.asLeft[Option[(I, C, Option[A])]].pure[F]
  end challenge

  def getDnsIdentifierAndChallenge(authorization: Authorization[Challenge]): Either[Error, (DNS, `dns-01`)] =
    for
      identifier <- typed[DNS](authorization.identifier).label("identifier")
      dnsChallenges: List[`dns-01`] = authorization.challenges.collect {
        case challenge: `dns-01` => challenge
      }
      given Show[`dns-01`] = Show.fromToString[`dns-01`]
      challenge <- one(dnsChallenges).label("dnsChallenges")
    yield
      (identifier, challenge)

  def createDNSRecord[DNSRecordId](identifier: DNS, challenge: `dns-01`, publicKey: PublicKey)
                                  (using dnsChallengeClient: DNSChallengeClient[F, DNSRecordId])
  : F[Either[Error, Option[DNSRecordId]]] =
    dnsChallengeClient.createDNSRecord(identifier, challenge, publicKey)

  def cleanDNSRecords[DNSRecordId](identifier: DNS, challenge: `dns-01`, dnsRecordId: Option[DNSRecordId])
                                  (using dnsChallengeClient: DNSChallengeClient[F, DNSRecordId])
  : F[Either[Error, List[DNSRecordId]]] =
    dnsChallengeClient.cleanDNSRecord(identifier, challenge, dnsRecordId)

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
      acmeApi = ACMEApi[F](locale, true, nonceRef, directoryRef)(client)(dsl)
    yield
      new ACMEClient[F, Challenge](directoryUri, directoryMaxAge, acmeApi, nonceRef, directoryRef)
end ACMEClient
