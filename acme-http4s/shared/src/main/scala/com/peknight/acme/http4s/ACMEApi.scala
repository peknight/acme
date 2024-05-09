package com.peknight.acme.http4s

import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import com.peknight.acme.circe.instances.directory.given
import com.peknight.acme.http4s.headers.{getHeaders, responseHeaders}
import com.peknight.acme.{Directory, Result, api}
import com.peknight.codec.configuration.given
import com.peknight.codec.http4s.circe.instances.entityDecoder.given
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Status, Uri}

import java.time.ZonedDateTime
import java.util.Locale

class ACMEApi[F[_]: Async](locale: Locale, compression: Boolean)(client: Client[F])(dsl: Http4sClientDsl[F])
  extends api.ACMEApi[F]:
  import dsl.*
  given CanEqual[Status, Status] = CanEqual.derived
  def directory(uri: Uri)(lastModified: Option[ZonedDateTime]): F[Result[Directory]] =
    for
      headers <- getHeaders[F](locale, compression, lastModified)
      result <- client.run(GET(uri, headers)).use { response =>
        val directory =
          if Status.NotModified == response.status then none[Directory].pure[F]
          else response.as[Directory].map(_.some)
        for
          dir <- directory
          headers <- responseHeaders[F](response.headers)
        yield
          Result(dir, headers)
      }
    yield result

end ACMEApi
