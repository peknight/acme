package com.peknight.acme.http4s

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor.*
import cats.syntax.traverse.*
import com.peknight.cats.effect.ext.Clock
import com.peknight.http4s.ext.MediaRange.{`application/jose+json`, `application/json`}
import org.http4s.*
import org.http4s.CacheDirective.`max-age`
import org.http4s.headers.*

import java.time.ZonedDateTime
import java.util.Locale
import scala.concurrent.duration.*

package object headers:

  private[this] val userAgent: `User-Agent` = `User-Agent`(ProductId("peknight/acme"), ProductComment("Scala"))
  private[this] val acceptCharset: `Accept-Charset` = `Accept-Charset`(CharsetRange.fromCharset(Charset.`UTF-8`))

  private[this] def localeToLanguageTag[F[_]: Sync](locale: Locale): F[NonEmptyList[LanguageTag]] =
    Sync[F].blocking(locale.toLanguageTag).map { langTag =>
      if "und" == langTag then NonEmptyList.one(LanguageTag.*)
      else
        val head = LanguageTag(langTag)
        val last = LanguageTag.*.withQValue(QValue.unsafeFromString("0.1")) :: Nil
        val tail =
          if langTag.contains('-') then LanguageTag(locale.getLanguage, QValue.unsafeFromString("0.8")) :: last
          else last
        NonEmptyList(head, tail)
    }

  private[this] def acceptLanguage[F[_]: Sync](locale: Locale): F[`Accept-Language`] =
    localeToLanguageTag[F](locale).map(`Accept-Language`.apply)

  private[this] val accept: Accept = Accept(MediaRangeAndQValue.withDefaultQValue(`application/json`))

  private[this] val acceptEncoding: `Accept-Encoding` = `Accept-Encoding`(ContentCoding.gzip)

  def headers[F[_]: Sync](locale: Locale, compression: Boolean): F[Headers] =
    acceptLanguage[F](locale).map(acceptLang =>
      Headers(userAgent, acceptCharset, acceptLang) ++ (if compression then Headers(acceptEncoding) else Headers.empty)
    )

  private[this] def ifModifiedSince(lastModified: ZonedDateTime): `If-Modified-Since` =
    `If-Modified-Since`(HttpDate.unsafeFromZonedDateTime(lastModified))

  def getHeaders[F[_]: Sync](locale: Locale, compression: Boolean, lastModified: Option[ZonedDateTime]): F[Headers] =
    headers(locale, compression).map(
      _ ++ Headers(accept) ++ lastModified.fold(Headers.empty)(last => Headers(ifModifiedSince(last)))
    )

  def postHeaders[F[_]: Sync](locale: Locale, compression: Boolean): F[Headers] =
    headers(locale, compression).map(_ ++ Headers(accept, `Content-Type`(`application/jose+json`)))

  def responseHeaders[F[_]: Sync](headers: Headers): F[com.peknight.acme.Headers] =
    given CanEqual[Duration, Duration] = CanEqual.derived
    val expiration = headers.get[`Cache-Control`]
      .flatMap { _.values.collectFirst {
        case `max-age`(deltaSeconds) if deltaSeconds != 0.second =>
          Clock.realTimeInstant[F].map(_.plusSeconds(deltaSeconds.toSeconds))
      }}
      .sequence
      .map(_.orElse(headers.get[Expires].map(expires => expires.expirationDate.toInstant))
        .map(ZonedDateTime.from)
      )
    val lastModified = headers.get[`Last-Modified`].map(last => ZonedDateTime.from(last.date.toInstant))
    expiration.map(exp => com.peknight.acme.Headers(lastModified, exp))
end headers
