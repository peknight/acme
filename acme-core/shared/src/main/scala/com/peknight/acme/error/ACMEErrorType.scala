package com.peknight.acme.error

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType
import org.http4s.Uri

enum ACMEErrorType:
  case
  // The request specified an account that does not exist [RFC8555]
  accountDoesNotExist,
  // The request specified a certificate to be revoked that has already been revoked [RFC8555]
  alreadyRevoked,
  // The CSR is unacceptable (e.g., due to a short key) [RFC8555]
  badCSR,
  // The client sent an unacceptable anti-replay nonce [RFC8555]
  badNonce,
  // The JWS was signed by a public key the server does not support [RFC8555]
  badPublicKey,
  // The revocation reason provided is not allowed by the server [RFC8555]
  badRevocationReason,
  // The JWS was signed with an algorithm the server does not support [RFC8555]
  badSignatureAlgorithm,
  // Certification Authority Authorization (CAA) records forbid the CA from issuing a certificate [RFC8555]
  caa,
  // Specific error conditions are indicated in the "subproblems" array [RFC8555]
  compound,
  // The server could not connect to validation target [RFC8555]
  connection,
  // There was a problem with a DNS query during identifier validation [RFC8555]
  dns,
  // The request must include a value for the "externalAccountBinding" field [RFC8555]
  externalAccountRequired,
  // Response received didn't match the challenge's requirements [RFC8555]
  incorrectResponse,
  // A contact URL for an account was invalid [RFC8555]
  invalidContact,
  // The request message was malformed [RFC8555]
  malformed,
  // The request attempted to finalize an order that is not ready to be finalized [RFC8555]
  orderNotReady,
  // The request exceeds a rate limit [RFC8555]
  rateLimited,
  // The server will not issue certificates for the identifier [RFC8555]
  rejectedIdentifier,
  // The server experienced an internal error [RFC8555]
  serverInternal,
  // The server received a TLS error during validation [RFC8555]
  tls,
  // The client lacks sufficient authorization [RFC8555]
  unauthorized,
  // A contact URL for an account used an unsupported protocol scheme [RFC8555]
  unsupportedContact,
  // An identifier is of an unsupported type [RFC8555]
  unsupportedIdentifier,
  // Visit the "instance" URL and take actions specified there [RFC8555]
  userActionRequired,
  // The short-term certificate is no longer available because the auto-renewal Order has been explicitly canceled by the IdO [RFC8739]
  autoRenewalCanceled,
  // The short-term certificate is no longer available because the auto-renewal Order has expired [RFC8739]
  autoRenewalExpired,
  // A request to cancel an auto-renewal Order that is not in state "valid" has been received [RFC8739]
  autoRenewalCancellationInvalid,
  // A request to revoke an auto-renewal Order has been received [RFC8739]
  autoRenewalRevocationNotSupported,
  // An unknown config is listed in the delegation attribute of the order request [RFC9115]
  unknownDelegation,
  // The CA only supports checking CAA for hidden services in-band, but the client has not provided an in-band CAA [RFC-ietf-acme-onion-07]
  onionCAARequired
  def `type`: Uri = Uri.unsafeFromString(s"urn:ietf:params:acme:error:$this")
end ACMEErrorType
object ACMEErrorType:
  given stringCodecACMEErrorType[F[_]: Applicative]: Codec[F, String, String, ACMEErrorType] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, ACMEErrorType](using CodecConfig.default)
  given codecACMEErrorType[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], ACMEErrorType] =
    Codec.codecS[F, S, ACMEErrorType]
end ACMEErrorType
