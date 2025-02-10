package com.peknight.acme.http4s

import cats.Id
import cats.data.EitherT
import cats.effect.{Async, Ref}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*
import com.peknight.acme.error.client.{NewNonceResponseStatus, NewNonceRateLimited}
import com.peknight.acme.headers.{baseHeaders, getHeaders}
import com.peknight.acme.http.HttpCache
import com.peknight.acme.syntax.headers.{getExpiration, getLastModified, getNonce, getRetryAfter}
import com.peknight.acme.{Directory, api}
import com.peknight.cats.effect.ext.Clock
import com.peknight.cats.instances.time.instant.given
import com.peknight.codec.Decoder
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.circe.instances.entityDecoder.given
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import io.circe.Json
import org.http4s.*
import org.http4s.Method.{GET, HEAD}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.time.Instant
import java.util.Locale

class ACMEApi[F[_]: Async](
                            locale: Locale,
                            compression: Boolean,
                            nonceRef: Ref[F, Option[Base64UrlNoPad]],
                            directoryRef: Ref[F, Option[HttpCache[Directory]]]
                          )(client: Client[F])(dsl: Http4sClientDsl[F])
  extends api.ACMEApi[F]:

  import dsl.*

  def directory(uri: Uri): F[Either[Error, Directory]] =
    get[Directory](directoryRef, "directory")(cacheOption =>
      getHeaders[F](locale, compression, cacheOption.flatMap(_.headers.getLastModified)).map(headers => GET(uri, headers))
    )

  def newNonce(uri: Uri): F[Either[Error, Base64UrlNoPad]] =
    val eitherF =
      for
        headers <- baseHeaders[F](locale, compression)
        either <- client.run(HEAD(uri, headers)).use { response =>
          if Status.Ok =!= response.status && Status.NoContent =!= response.status then
            response.headers.getRetryAfter match
              case Some(retryAfter) => NewNonceRateLimited(response.status, retryAfter).asLeft.pure[F]
              case _ => NewNonceResponseStatus(response.status).asLeft.pure[F]
          else
            response.headers.getNonce.toRight(OptionEmpty.label("nonce")).pure[F]
        }
      yield
        either
    eitherF.asError.map(_.flatten)

  def resetNonce(uri: Uri): F[Either[Error, Unit]] =
    val eitherT =
      for
        _ <- EitherT(nonceRef.set(none[Base64UrlNoPad]).asError)
        nonce <- EitherT(newNonce(uri))
        _ <- EitherT(nonceRef.set(nonce.some).asError)
      yield
        ()
    eitherT.value

  private def updateNonce(response: Response[F]): F[Unit] =
    response.headers.getNonce match
      case Some(nonce) => nonceRef.set(nonce.some)
      case _ => ().pure[F]

  private def getFromCache[A](cacheOption: Option[HttpCache[A]]): F[Option[A]] =
    cacheOption match
      case Some(HttpCache(_, Some(expiration), value)) =>
        Clock.realTimeInstant[F].map(now => if now < expiration then value.some else none[A])
      case _ => none[A].pure[F]

  private def get[A](cacheRef: Ref[F, Option[HttpCache[A]]], label: String)
                    (requestF: Option[HttpCache[A]] => F[Request[F]])
                    (using Decoder[Id, Cursor[Json], A])
  : F[Either[Error, A]] =
    val eitherF =
      for
        cacheOption <- cacheRef.get
        valueOption <- getFromCache(cacheOption)
        either <- valueOption match
          case Some(value) => value.asRight[Error].pure[F]
          case _ =>
            for
              request <- requestF(cacheOption)
              either <- client.run(request).use(response => updateNonce(response).flatMap { _ =>
                if Status.NotModified === response.status then
                  cacheOption.map(_.value).toRight(OptionEmpty.label(label)).pure[F]
                else
                  for
                    value <- response.as[A]
                    expiration <- response.headers.getExpiration[F]
                    _ <- cacheRef.set(HttpCache(response.headers, expiration, value).some)
                  yield
                    value.asRight
              })
            yield
              either
      yield
        either
    eitherF.asError.map(_.flatten)

end ACMEApi
