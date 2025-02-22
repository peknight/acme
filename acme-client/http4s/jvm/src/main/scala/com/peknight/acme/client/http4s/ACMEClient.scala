package com.peknight.acme.client.http4s

import cats.MonadError
import cats.data.EitherT
import cats.effect.Ref
import cats.syntax.option.*
import com.peknight.acme.client.api
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asError
import org.http4s.Uri

class ACMEClient[F[_]](
                        acmeApi: api.ACMEApi[F],
                        nonceRef: Ref[F, Option[Base64UrlNoPad]],
                      )(using MonadError[F, Throwable]) extends api.ACMEClient[F]:
  def resetNonce(uri: Uri): F[Either[Error, Unit]] =
    val eitherT =
      for
        _ <- EitherT(nonceRef.set(none[Base64UrlNoPad]).asError)
        nonce <- EitherT(acmeApi.newNonce(uri))
        _ <- EitherT(nonceRef.set(nonce.some).asError)
      yield
        ()
    eitherT.value
end ACMEClient
