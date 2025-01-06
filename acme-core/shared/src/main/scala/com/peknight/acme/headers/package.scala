package com.peknight.acme

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor.*
import com.peknight.http4s.ext.MediaRange.`application/json`
import com.peknight.jose.http4s.MediaRange.`application/jose+json`
import org.http4s.*
import org.http4s.headers.*

import java.time.Instant
import java.util.Locale

package object headers:
  private val userAgent: `User-Agent` = `User-Agent`(ProductId("peknight/acme"), ProductComment("Scala"))
  private val acceptCharset: `Accept-Charset` = `Accept-Charset`(CharsetRange.fromCharset(Charset.`UTF-8`))

  private def localeToLanguageTag[F[_]: Sync](locale: Locale): F[NonEmptyList[LanguageTag]] =
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

  private def acceptLanguage[F[_]: Sync](locale: Locale): F[`Accept-Language`] =
    localeToLanguageTag[F](locale).map(`Accept-Language`.apply)


  private val acceptEncoding: `Accept-Encoding` = `Accept-Encoding`(ContentCoding.gzip)

  def headers[F[_]: Sync](locale: Locale, compression: Boolean): F[Headers] =
    acceptLanguage[F](locale).map(acceptLang =>
      Headers(userAgent, acceptCharset, acceptLang) ++ (if compression then Headers(acceptEncoding) else Headers.empty)
    )

  private val accept: Accept = Accept(MediaRangeAndQValue.withDefaultQValue(`application/json`))

  private def ifModifiedSince(lastModified: Instant): `If-Modified-Since` =
    `If-Modified-Since`(HttpDate.unsafeFromInstant(lastModified))

  def getHeaders[F[_]: Sync](locale: Locale, compression: Boolean, lastModified: Option[Instant]): F[Headers] =
    headers(locale, compression).map(
      _ ++ Headers(accept) ++ lastModified.fold(Headers.empty)(last => Headers(ifModifiedSince(last)))
    )

  def postHeaders[F[_]: Sync](locale: Locale, compression: Boolean): F[Headers] =
    headers(locale, compression).map(_ ++ Headers(accept, `Content-Type`(`application/jose+json`)))
end headers
