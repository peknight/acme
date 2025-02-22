package com.peknight.acme.client.http4s

import cats.data.EitherT
import cats.effect.{Async, Ref, Sync}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import com.peknight.acme.account.{AccountClaims, NewAccountHttpResponse}
import com.peknight.acme.client.api
import com.peknight.acme.client.error.CanNotCombineWithAutoRenewal
import com.peknight.acme.client.jose.createJoseRequest
import com.peknight.acme.directory.Directory
import com.peknight.acme.order.OrderClaims
import com.peknight.cats.effect.ext.Clock
import com.peknight.cats.ext.syntax.eitherT.{eLiftET, rLiftET}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.commons.time.syntax.temporal.plus
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.http.HttpResponse
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.security.KeyPair
import java.util.Locale
import scala.concurrent.duration.*

class ACMEClient[F[_]: Sync](
                              directoryUri: Uri,
                              directoryMaxAge: FiniteDuration,
                              acmeApi: api.ACMEApi[F],
                              nonceRef: Ref[F, Option[Base64UrlNoPad]],
                              directoryRef: Ref[F, Option[HttpResponse[Directory]]]
                            ) extends api.ACMEClient[F]:
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
        jws <- EitherT(createJoseRequest[F, AccountClaims](directory.newAccount, claims, keyPair, Some(nonce)))
        response <- EitherT(acmeApi.newAccount(jws, directory.newAccount))
      yield
        response
    eitherT.value

  def newOrder(claims: OrderClaims): F[Either[Error, Unit]] =
    val eitherT =
      for
        _ <- claims.autoRenewal.fold(().asRight[Error]) { autoRenewal => (claims.notBefore, claims.notAfter) match
          case (Some(_), Some(_)) => CanNotCombineWithAutoRenewal("notBefore&notAfter").asLeft
          case (Some(_), None) => CanNotCombineWithAutoRenewal("notBefore").asLeft
          case (None, Some(_)) => CanNotCombineWithAutoRenewal("notAfter").asLeft
          case _ => ().asRight
        }.eLiftET
      yield
        ()
    eitherT.value
end ACMEClient
object ACMEClient:
  def apply[F[_]: Async](client: Client[F], directoryUri: Uri, directoryMaxAge: FiniteDuration = 10.minutes)
                        (dsl: Http4sClientDsl[F]): F[api.ACMEClient[F]] =
    for
      locale <- Sync[F].blocking(Locale.getDefault)
      nonceRef <- Ref[F].of(none[Base64UrlNoPad])
      directoryRef <- Ref[F].of(none[HttpResponse[Directory]])
      acmeApi = ACMEApi[F](locale, true, nonceRef, directoryRef)(client)(dsl)
    yield
      new ACMEClient[F](directoryUri, directoryMaxAge, acmeApi, nonceRef, directoryRef)
end ACMEClient
