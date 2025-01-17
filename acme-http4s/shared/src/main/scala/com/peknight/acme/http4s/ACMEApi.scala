package com.peknight.acme.http4s

import cats.effect.{Async, Ref}
import cats.syntax.applicative.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import com.peknight.acme.headers.getHeaders
import com.peknight.acme.syntax.headers.{getLastModified, getNonce}
import com.peknight.acme.{Directory, Result, api}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.http4s.circe.instances.entityDecoder.given
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Request, Response, Status, Uri}

import java.util.Locale

class ACMEApi[F[_]: Async](locale: Locale, compression: Boolean, directoryRef: Ref[F, Result[Directory]],
                           nonceRef: Ref[F, Option[Base64UrlNoPad]])(client: Client[F])(dsl: Http4sClientDsl[F])
  extends api.ACMEApi[F]:
  import dsl.*
  def directory(uri: Uri): F[Result[Directory]] =
    for
      directoryRes <- directoryRef.get
      headers <- getHeaders[F](locale, compression, directoryRes.body.flatMap(_ => directoryRes.headers.getLastModified))
      result <- run(GET(uri, headers)){ response =>
        if Status.NotModified === response.status then directoryRes.pure[F]
        else
          for
            dir <- response.as[Directory]
            result = Result(response.headers, dir.some)
            _ <- directoryRef.set(result)
          yield
            result
      }
    yield result

  private def run[A](req: Request[F])(f: Response[F] => F[A]): F[A] =
    client.run(req).use { response =>
      for
        _ <- response.headers.getNonce.fold(().pure[F])(nonce => nonceRef.set(nonce.some))
        result <- f(response)
      yield
        result
    }
end ACMEApi
