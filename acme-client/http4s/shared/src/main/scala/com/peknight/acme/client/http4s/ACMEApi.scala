package com.peknight.acme.client.http4s

import cats.Id
import cats.effect.{Async, Ref}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*
import com.peknight.acme.account.{NewAccountHttpResponse, NewAccountResponse}
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.client.api
import com.peknight.acme.client.headers.{baseHeaders, getHeaders, postHeaders}
import com.peknight.acme.directory.Directory
import com.peknight.acme.error.ACMEError
import com.peknight.acme.order.{NewOrderHttpResponse, Order}
import com.peknight.acme.syntax.headers.getNonce
import com.peknight.cats.effect.ext.Clock
import com.peknight.cats.instances.time.instant.given
import com.peknight.codec.Decoder
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.circe.instances.entityDecoder.given
import com.peknight.codec.syntax.encoder.asS
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.http.HttpResponse
import com.peknight.http4s.ext.syntax.headers.{getLastModified, getLocation}
import com.peknight.jose.jws.JsonWebSignature
import io.circe.Json
import org.http4s.*
import org.http4s.Method.{GET, HEAD, POST}
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.time.Instant
import java.util.Locale

class ACMEApi[F[_]: Async](
                            locale: Locale,
                            compression: Boolean,
                            nonceRef: Ref[F, Option[Base64UrlNoPad]],
                            directoryRef: Ref[F, Option[HttpResponse[Directory]]]
                          )(client: Client[F])(dsl: Http4sClientDsl[F])
  extends api.ACMEApi[F]:

  import dsl.*

  def directory(uri: Uri): F[Either[Error, Directory]] = get[Directory](uri, directoryRef, "directory")

  def newNonce(uri: Uri): F[Either[Error, Base64UrlNoPad]] =
    val eitherF =
      for
        headers <- baseHeaders[F](locale, compression)
        either <- client.run(HEAD(uri, headers)).use(response =>
          if response.status.isSuccess then response.headers.getNonce.toRight(OptionEmpty.label("nonce")).pure
          else response.as[ACMEError].map(_.asLeft)
        )
      yield
        either
    eitherF.asError.map(_.flatten)

  def newAccount(jws: JsonWebSignature, uri: Uri): F[Either[Error, NewAccountHttpResponse]] =
    postJwsWithLocation[NewAccountResponse, NewAccountHttpResponse](jws, uri, "accountLocation")(
      NewAccountHttpResponse.apply
    )

  def account(jws: JsonWebSignature, uri: Uri): F[Either[Error, NewAccountResponse]] =
    postJws[NewAccountResponse](jws, uri).map(_.map(_.body))

  def newOrder(jws: JsonWebSignature, uri: Uri): F[Either[Error, NewOrderHttpResponse]] =
    postJwsWithLocation[Order, NewOrderHttpResponse](jws, uri, "orderLocation")(NewOrderHttpResponse.apply)

  def order(jws: JsonWebSignature, uri: Uri): F[Either[Error, Order]] =
    postJws[Order](jws, uri).map(_.map(_.body))

  def authorization[Challenge](jws: JsonWebSignature, uri: Uri)(using Decoder[Id, Cursor[Json], Challenge])
  : F[Either[Error, Authorization[Challenge]]] =
    postJws[Authorization[Challenge]](jws, uri).map(_.map(_.body))

  def challenge[Challenge](jws: JsonWebSignature, uri: Uri)(using Decoder[Id, Cursor[Json], Challenge])
  : F[Either[Error, HttpResponse[Challenge]]] =
    postJws[Challenge](jws, uri)

  private def get[A](uri: Uri, cacheRef: Ref[F, Option[HttpResponse[A]]], label: String)
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
              headers <- getHeaders[F](locale, compression, cacheOption.flatMap(_.headers.getLastModified))
              either <- client.run(GET(uri, headers)).use(response => updateNonce(response).flatMap(_ =>
                if Status.NotModified === response.status then
                  cacheOption.map(_.body).toRight(OptionEmpty.label(label)).pure
                else if response.status.isSuccess then
                  for
                    resp <- HttpResponse.fromResponse[F, A](response)
                    _ <- cacheRef.set(resp.some)
                  yield
                    resp.body.asRight
                else response.as[ACMEError].map(_.asLeft)
              ))
            yield
              either
      yield
        either
    eitherF.asError.map(_.flatten)

  private def getFromCache[A](cacheOption: Option[HttpResponse[A]]): F[Option[A]] =
    cacheOption match
      case Some(HttpResponse(_, _, body, Some(expiration))) =>
        Clock.realTimeInstant[F].map(now => if now < expiration then body.some else none[A])
      case _ => none[A].pure[F]

  private def updateNonce(response: Response[F]): F[Unit] =
    response.headers.getNonce match
      case Some(nonce) => nonceRef.set(nonce.some)
      case _ => ().pure[F]

  private def postJwsWithLocation[A, B](jws: JsonWebSignature, uri: Uri, locationLabel: String)
                                       (f: (A, Uri) => B)
                                       (using Decoder[Id, Cursor[Json], A]): F[Either[Error, B]] =
    postJws[A](jws, uri).map(_.flatMap {
      case HttpResponse(_, headers, body, _) =>
        headers.getLocation(uri)
          .toRight(OptionEmpty.label(locationLabel))
          .map(location => f(body, location))
    })

  private def postJws[A](jws: JsonWebSignature, uri: Uri)(using Decoder[Id, Cursor[Json], A])
  : F[Either[Error, HttpResponse[A]]] =
    val eitherF =
      for
        headers <- postHeaders[F](locale, compression)
        result <- client.run(POST(jws.asS[Id, Json], uri, headers)).use(response => updateNonce(response).flatMap(_ =>
          if response.status.isSuccess then HttpResponse.fromResponse[F, A](response).map(_.asRight)
          else response.as[ACMEError].map(_.asLeft)
        ))
      yield
        result
    eitherF.asError.map(_.flatten)
end ACMEApi
