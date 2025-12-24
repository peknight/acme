package com.peknight.acme.client.letsencrypt.challenge

import cats.data.EitherT
import cats.effect.Sync
import com.peknight.acme.client.letsencrypt.challenge.Challenge.`http-01`
import com.peknight.cats.syntax.eitherT.eLiftET
import com.peknight.error.Error
import com.peknight.jose.jwk.JsonWebKey

import java.security.PublicKey

trait HTTP01Platform { self: `http-01` =>
  def content[F[_]: Sync](publicKey: PublicKey): F[Either[Error, String]] =
    val eitherT =
      for
        jwk <- JsonWebKey.fromKey(publicKey).eLiftET[F]
        thumbprint <- EitherT(jwk.calculateBase64UrlEncodedThumbprint[F]())
      yield
        s"${self.token.value}.${thumbprint.value}"
    eitherT.value
}
