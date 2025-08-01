package com.peknight.acme.client.letsencrypt.challenge

import cats.data.EitherT
import cats.effect.Sync
import com.peknight.acme.client.letsencrypt.challenge.Challenge.`dns-01`
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asET
import com.peknight.error.syntax.either.asError
import com.peknight.jose.jwk.JsonWebKey
import com.peknight.security.digest.`SHA-256`
import scodec.bits.ByteVector

import java.security.PublicKey

trait DNS01Platform { self: `dns-01` =>
  def content[F[_]: Sync](publicKey: PublicKey): F[Either[Error, String]] =
    val eitherT =
      for
        jwk <- JsonWebKey.fromKey(publicKey).eLiftET[F]
        thumbprint <- EitherT(jwk.calculateBase64UrlEncodedThumbprint[F]())
        authorization = s"${self.token.value}.${thumbprint.value}"
        input <- ByteVector.encodeUtf8(authorization).asError.eLiftET[F]
        sha256hash <- `SHA-256`.digest[F](input).asET
      yield
        Base64UrlNoPad.fromByteVector(sha256hash).value
    eitherT.value
}

