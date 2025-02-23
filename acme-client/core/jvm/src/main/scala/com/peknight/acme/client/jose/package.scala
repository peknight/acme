package com.peknight.acme.client

import cats.Id
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.either.*
import com.peknight.acme.jose.JWSHeaderExt
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.codec.Encoder
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import com.peknight.jose.error.{UnsupportedCurve, UnsupportedKeyAlgorithm}
import com.peknight.jose.jwa.JsonWebAlgorithm
import com.peknight.jose.jwa.ecc.{`P-256K`, `P-256`, `P-384`, `P-521`}
import com.peknight.jose.jwa.signature.*
import com.peknight.jose.jwk.JsonWebKey.{AsymmetricJsonWebKey, EllipticCurveJsonWebKey, RSAJsonWebKey}
import com.peknight.jose.jwk.{JsonWebKey, KeyId}
import com.peknight.jose.jws.JsonWebSignature
import com.peknight.jose.jwx.{JoseConfiguration, JoseHeader}
import io.circe.Json
import org.http4s.Uri

import java.security.{KeyPair, PrivateKey, PublicKey}
import javax.crypto.SecretKey

package object jose:
  def signJson[F[_], A](url: Uri, payload: A, keyPair: KeyPair,
                        nonce: Option[Base64UrlNoPad] = None, keyId: Option[KeyId] = None)
                       (using Sync[F], Encoder[Id, Json, A]): F[Either[Error, JsonWebSignature]] =
    sign[F](url, keyPair, nonce, keyId)((header, key) => JsonWebSignature.signJson[F, A](header, payload, key))

  def signString[F[_]: Sync](url: Uri, payload: String, keyPair: KeyPair, nonce: Option[Base64UrlNoPad] = None,
                             keyId: Option[KeyId] = None): F[Either[Error, JsonWebSignature]] =
    sign[F](url, keyPair, nonce, keyId)((header, key) => JsonWebSignature.signString[F](header, payload, key))

  private def sign[F[_]: Sync](url: Uri, keyPair: KeyPair, nonce: Option[Base64UrlNoPad] = None,
                               keyId: Option[KeyId] = None)
                              (f: (JoseHeader, Option[PrivateKey]) => F[Either[Error, JsonWebSignature]])
  : F[Either[Error, JsonWebSignature]] =
    val eitherT =
      for
        jwk <- JsonWebKey.fromPublicKey(keyPair.getPublic).eLiftET[F]
        algorithm <- keyAlgorithm(jwk).eLiftET[F]
        header = keyId match
          case Some(keyId) => JoseHeader.withExt(JWSHeaderExt(url, nonce), Some(algorithm), keyID = Some(keyId))
          case _ => JoseHeader.withExt(JWSHeaderExt(url, nonce), Some(algorithm), jwk = Some(jwk))
        jws <- EitherT(f(header, Some(keyPair.getPrivate)))
        jws <- jws.excludeHeader.eLiftET[F]
      yield
        jws
    eitherT.value

  def createExternalAccountBinding[F[_]: Sync](macAlgorithm: HmacSHA, keyId: KeyId, url: Uri, accountKey: PublicKey,
                                               macKey: SecretKey)
  : F[Either[Error, JsonWebSignature]] =
    val eitherT =
      for
        keyJwk <- JsonWebKey.fromPublicKey(accountKey).eLiftET[F]
        jws <- EitherT(JsonWebSignature.signJson[F, AsymmetricJsonWebKey](JoseHeader.withExt(JWSHeaderExt(url),
          Some(macAlgorithm), keyID = Some(keyId)), keyJwk, Some(macKey),
          JoseConfiguration(doKeyValidation = false)))
        jws <- jws.excludeHeader.eLiftET[F]
      yield
        jws
    eitherT.value

  def keyAlgorithm(jwk: JsonWebKey): Either[Error, JsonWebAlgorithm] =
    jwk match
      case ecJwk: EllipticCurveJsonWebKey => ecJwk.curve match
        case `P-256` => ES256.asRight
        case `P-256K` => ES256K.asRight
        case `P-384` => ES384.asRight
        case `P-521` => ES512.asRight
        case curve => UnsupportedCurve(curve).asLeft
      case rsaJwk: RSAJsonWebKey => RS256.asRight
      case _ => UnsupportedKeyAlgorithm(jwk.algorithm.map(_.identifier).getOrElse(jwk.keyType.name)).asLeft
end jose
