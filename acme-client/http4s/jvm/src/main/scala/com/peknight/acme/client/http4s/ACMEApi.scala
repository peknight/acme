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
import com.peknight.acme.account.Account
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.client.api
import com.peknight.acme.client.headers.{baseHeaders, getHeaders, postHeaders}
import com.peknight.acme.directory.Directory
import com.peknight.acme.error.ACMEError
import com.peknight.acme.order.Order
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
import com.peknight.http4s.ext.media.MediaRange.`application/json`
import com.peknight.http4s.ext.syntax.headers.{getLastModified, getLocation}
import com.peknight.jose.jws.JsonWebSignature
import com.peknight.security.http4s.instances.x509Certificate.given
import com.peknight.security.http4s.media.MediaRange.`application/pem-certificate-chain`
import io.circe.Json
import org.http4s.*
import org.http4s.Method.{GET, HEAD, POST}
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.security.cert.X509Certificate
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

  def newAccount(jws: JsonWebSignature, uri: Uri): F[Either[Error, (Account, Uri)]] =
    postJwsWithLocation[Account](jws, uri, "accountLocation")

  def account(jws: JsonWebSignature, uri: Uri): F[Either[Error, Account]] =
    postJwsAcceptJson[Account](jws, uri).map(_.map(_.body))

  def newOrder(jws: JsonWebSignature, uri: Uri): F[Either[Error, (Order, Uri)]] =
    postJwsWithLocation[Order](jws, uri, "orderLocation")

  def order(jws: JsonWebSignature, uri: Uri): F[Either[Error, HttpResponse[Order]]] =
    postJwsAcceptJson[Order](jws, uri)

  def finalizeOrder(jws: JsonWebSignature, uri: Uri): F[Either[Error, HttpResponse[Order]]] =
    postJwsAcceptJson[Order](jws, uri)

  def authorization[Challenge](jws: JsonWebSignature, uri: Uri)(using Decoder[Id, Cursor[Json], Challenge])
  : F[Either[Error, Authorization[Challenge]]] =
    postJwsAcceptJson[Authorization[Challenge]](jws, uri).map(_.map(_.body))

  def challenge[Challenge](jws: JsonWebSignature, uri: Uri)(using Decoder[Id, Cursor[Json], Challenge])
  : F[Either[Error, HttpResponse[Challenge]]] =
    postJwsAcceptJson[Challenge](jws, uri)

  def certificates(jws: JsonWebSignature, uri: Uri): F[Either[Error, HttpResponse[List[X509Certificate]]]] =
    postJws[List[X509Certificate]](jws, uri, `application/pem-certificate-chain`)

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

  private def postJwsWithLocation[A](jws: JsonWebSignature, uri: Uri, locationLabel: String)
                                    (using Decoder[Id, Cursor[Json], A]): F[Either[Error, (A, Uri)]] =
    postJwsAcceptJson[A](jws, uri).map(_.flatMap {
      case HttpResponse(_, headers, body, _) =>
        headers.getLocation(uri)
          .toRight(OptionEmpty.label(locationLabel))
          .map(location => (body, location))
    })

  private def postJwsAcceptJson[A](jws: JsonWebSignature, uri: Uri)(using EntityDecoder[F, A])
  : F[Either[Error, HttpResponse[A]]] =
    postJws[A](jws, uri, `application/json`)

  private def postJws[A](jws: JsonWebSignature, uri: Uri, acceptMediaRange: MediaRange)(using EntityDecoder[F, A])
  : F[Either[Error, HttpResponse[A]]] =
    val eitherF =
      for
        headers <- postHeaders[F](locale, compression, acceptMediaRange)
        result <- client.run(POST(jws.asS[Id, Json], uri, headers)).use(response => updateNonce(response).flatMap(_ =>
          if response.status.isSuccess then HttpResponse.fromResponse[F, A](response).map(_.asRight)
          else response.as[ACMEError].map(_.asLeft)
        ))
      yield
        result
    eitherF.asError.map(_.flatten)
end ACMEApi
