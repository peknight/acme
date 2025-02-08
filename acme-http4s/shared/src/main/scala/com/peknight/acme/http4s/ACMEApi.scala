package com.peknight.acme.http4s

import cats.effect.{Async, Ref}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import com.peknight.acme.headers.getHeaders
import com.peknight.acme.syntax.headers.{getLastModified, getNonce}
import com.peknight.acme.{Directory, api}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.http4s.circe.instances.entityDecoder.given
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import org.http4s.*
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.util.Locale

class ACMEApi[F[_]: Async](locale: Locale, compression: Boolean, directoryRef: Ref[F, Option[(Headers, Directory)]],
                           nonceRef: Ref[F, Option[Base64UrlNoPad]])(client: Client[F])(dsl: Http4sClientDsl[F])
  extends api.ACMEApi[F]:

  import dsl.*

  def directory(uri: Uri): F[Either[Error, Directory]] =
    val eitherF =
      for
        directoryR <- directoryRef.get
        headers <- getHeaders[F](locale, compression, directoryR.flatMap((headers, _) => headers.getLastModified))
        either <- run(GET(uri, headers)) { response =>
          if Status.NotModified === response.status then
            directoryR.map(_._2).toRight(OptionEmpty.label("directory")).pure[F]
          else
            for
              dir <- response.as[Directory]
              _ <- directoryRef.set((response.headers, dir).some)
            yield
              dir.asRight
        }
      yield either
    eitherF.asError.map(_.flatten)

  private def run[A](req: Request[F])(f: Response[F] => F[Either[Error, A]]): F[Either[Error, A]] =
    client.run(req).use { response =>
      for
        _ <- response.headers.getNonce.fold(().pure[F])(nonce => nonceRef.set(nonce.some))
        either <- f(response)
      yield
        either
    }
end ACMEApi
