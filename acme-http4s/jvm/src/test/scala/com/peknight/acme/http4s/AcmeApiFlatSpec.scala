package com.peknight.acme.http4s

import cats.data.NonEmptyList
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.comcast.ip4s.Host
import org.http4s.*
import org.http4s.Method.GET
import org.http4s.client.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.*
import org.http4s.syntax.literals.*
import org.scalatest.flatspec.AsyncFlatSpec

import java.time.ZonedDateTime
import java.util.Locale
import scala.concurrent.duration.*

class AcmeApiFlatSpec extends AsyncFlatSpec with AsyncIOSpec:

  val staging = uri"https://acme-staging-v02.api.letsencrypt.org/directory"
  val `application/json` = MediaRangeAndQValue.withDefaultQValue(MediaType.unsafeParse("application/json"))
  val userAgent = `User-Agent`(ProductId("peknight/acme"), ProductComment("Scala"))
  val acceptCharset = `Accept-Charset`(CharsetRange.fromCharset(Charset.`UTF-8`))
  def acceptLanguage[F[_]: Sync] =
    for
      locale <- Sync[F].blocking(Locale.getDefault)
      languageTags <- localeToLanguageTag[F](locale)
    yield
      `Accept-Language`(languageTags)
  val accept = Accept(`application/json`)
  def headers[F[_]: Sync](lastModified: Option[ZonedDateTime]): F[Headers] =
    for
      acceptLang <- acceptLanguage[F]
    yield
      Headers(
        userAgent,
        acceptCharset,
        acceptLang,
        accept,
      ) ++ lastModified.fold(Headers.empty)(last => Headers(`If-Modified-Since`(HttpDate.unsafeFromZonedDateTime(last))))
  def localeToLanguageTag[F[_]: Sync](locale: Locale): F[NonEmptyList[LanguageTag]] =
    Sync[F].blocking(locale.toLanguageTag).map { langTag =>
      if "und" == langTag then NonEmptyList.one(LanguageTag.*)
      else
        val head = LanguageTag(langTag)
        val last = LanguageTag.*.withQValue(QValue.unsafeFromString("0.1"))
        if langTag.contains('-') then
          NonEmptyList(head, List(LanguageTag(locale.getLanguage, QValue.unsafeFromString("0.8")), last))
        else NonEmptyList(head, List(last))
    }

  case class Meta(caaIdentities: List[Host], termsOfService: Uri, website: Uri)
  case class Directory(keyChange: Uri, meta: Meta, newAccount: Uri, newNonce: Uri, newOrder: Uri, renewalInfo: Uri, revokeCert: Uri)

  "ACME Api Directory" should "succeed" in {
    EmberClientBuilder.default[IO].withTimeout(10.seconds).build
      .use(client => headers[IO](None).flatMap(headers => client.run(GET(staging, headers)).use(_.as[String])))
      .asserting(result =>
        println(result)
        assert(true)
      )
  }

  val json = """
    |{
    |  "GeohYyzTlUk": "https://community.letsencrypt.org/t/adding-random-entries-to-the-directory/33417",
    |  "keyChange": "https://acme-staging-v02.api.letsencrypt.org/acme/key-change",
    |  "meta": {
    |    "caaIdentities": [
    |      "letsencrypt.org"
    |    ],
    |    "termsOfService": "https://letsencrypt.org/documents/LE-SA-v1.4-April-3-2024.pdf",
    |    "website": "https://letsencrypt.org/docs/staging-environment/"
    |  },
    |  "newAccount": "https://acme-staging-v02.api.letsencrypt.org/acme/new-acct",
    |  "newNonce": "https://acme-staging-v02.api.letsencrypt.org/acme/new-nonce",
    |  "newOrder": "https://acme-staging-v02.api.letsencrypt.org/acme/new-order",
    |  "renewalInfo": "https://acme-staging-v02.api.letsencrypt.org/draft-ietf-acme-ari-02/renewalInfo/",
    |  "revokeCert": "https://acme-staging-v02.api.letsencrypt.org/acme/revoke-cert"
    |}
    |""".stripMargin
end AcmeApiFlatSpec
