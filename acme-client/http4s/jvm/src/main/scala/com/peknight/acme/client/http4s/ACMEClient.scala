package com.peknight.acme.client.http4s

import cats.Id
import cats.data.EitherT
import cats.effect.{Async, Ref, Sync}
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import com.peknight.acme.account.{AccountClaims, NewAccountHttpResponse, NewAccountResponse}
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.client.api
import com.peknight.acme.client.error.*
import com.peknight.acme.client.jose.{signEmptyString, signJson}
import com.peknight.acme.directory.Directory
import com.peknight.acme.identifier.IdentifierType.dns
import com.peknight.acme.order.{NewOrderHttpResponse, Order, OrderClaims}
import com.peknight.cats.effect.ext.Clock
import com.peknight.cats.ext.syntax.eitherT.{lLiftET, rLiftET}
import com.peknight.codec.Decoder
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.commons.time.syntax.temporal.plus
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.http.HttpResponse
import com.peknight.jose.jwk.KeyId
import com.peknight.jose.jws.JsonWebSignature
import io.circe.Json
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.security.KeyPair
import java.util.Locale
import scala.concurrent.duration.*

class ACMEClient[F[_], Challenge](
                                   directoryUri: Uri,
                                   directoryMaxAge: FiniteDuration,
                                   acmeApi: api.ACMEApi[F],
                                   nonceRef: Ref[F, Option[Base64UrlNoPad]],
                                   directoryRef: Ref[F, Option[HttpResponse[Directory]]]
                                 )(using Sync[F], Decoder[Id, Cursor[Json], Challenge])
  extends api.ACMEClient[F, Challenge]:
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
        nonce <- EitherT(nonce)
        jws <- EitherT(signJson[F, AccountClaims](directory.newAccount, claims, keyPair, Some(nonce)))
        response <- EitherT(acmeApi.newAccount(jws, directory.newAccount))
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
  def apply[F[_], Challenge](client: Client[F], directoryUri: Uri, directoryMaxAge: FiniteDuration = 10.minutes)
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
