package com.peknight.acme.jose

import cats.data.EitherT
import cats.effect.Sync
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.error.Error
import com.peknight.jose.jwa.signature.HmacSHA
import com.peknight.jose.jwk.JsonWebKey.AsymmetricJsonWebKey
import com.peknight.jose.jwk.{JsonWebKey, KeyId}
import com.peknight.jose.jws.JsonWebSignature
import com.peknight.jose.jwx.{JoseConfiguration, JoseHeader}
import io.circe.{Json, JsonObject}
import org.http4s.Uri

import java.security.{KeyPair, PublicKey}
import javax.crypto.SecretKey

package object account:

  def createLogin[F[_]: Sync](keyPair: KeyPair): F[Either[Error, Unit]] =
    ???

  def createExternalAccountBinding[F[_]: Sync](keyId: KeyId, accountKey: PublicKey, macKey: SecretKey,
                                               macAlgorithm: HmacSHA, resource: Uri)
  : F[Either[Error, JsonWebSignature]] =
    val eitherT =
      for
        keyJwk <- JsonWebKey.fromPublicKey(accountKey).eLiftET[F]
        innerJws <- EitherT(JsonWebSignature.signJson[F, AsymmetricJsonWebKey](JoseHeader(Some(macAlgorithm),
          keyID = Some(keyId), ext = JsonObject("url" -> Json.fromString(resource.renderString))), keyJwk, Some(macKey),
          JoseConfiguration(doKeyValidation = false)))
        innerJws <- innerJws.excludeHeader.eLiftET[F]
      yield
        innerJws
    eitherT.value
end account
