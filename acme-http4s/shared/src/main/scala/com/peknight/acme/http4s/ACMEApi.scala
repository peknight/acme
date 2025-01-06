package com.peknight.acme.http4s

import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import com.peknight.acme.headers.getHeaders
import com.peknight.acme.{Directory, Result, api}
import com.peknight.codec.http4s.circe.instances.entityDecoder.given
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Status, Uri}

import java.time.Instant
import java.util.Locale

class ACMEApi[F[_]: Async](locale: Locale, compression: Boolean)(client: Client[F])(dsl: Http4sClientDsl[F])
  extends api.ACMEApi[F]:
  import dsl.*
  def directory(uri: Uri)(lastModified: Option[Instant]): F[Result[Directory]] =
    for
      headers <- getHeaders[F](locale, compression, lastModified)
      result <- client.run(GET(uri, headers)).use { response =>
        if Status.NotModified === response.status then Result(response.headers, none[Directory]).pure[F]
        else response.as[Directory].map(dir => Result(response.headers, dir.some))
      }
    yield result
end ACMEApi
