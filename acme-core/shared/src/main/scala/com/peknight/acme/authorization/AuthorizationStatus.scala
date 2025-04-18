package com.peknight.acme.authorization

import cats.{Applicative, Eq, Show}
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType
import com.peknight.codec.{Codec, Decoder, Encoder}

/**
 * Authorization objects are created in the "pending" state.  If one of
 * the challenges listed in the authorization transitions to the "valid"
 * state, then the authorization also changes to the "valid" state.  If
 * the client attempts to fulfill a challenge and fails, or if there is
 * an error while the authorization is still pending, then the
 * authorization transitions to the "invalid" state.  Once the
 * authorization is in the "valid" state, it can expire ("expired"), be
 * deactivated by the client ("deactivated", see Section 7.5.2), or
 * revoked by the server ("revoked").
 *
 *
 *                pending --------------------+
 *                   |                        |
 * Challenge failure |                        |
 *        or         |                        |
 *       Error       |  Challenge valid       |
 *         +---------+---------+              |
 *         |                   |              |
 *         V                   V              |
 *      invalid              valid            |
 *                             |              |
 *                             |              |
 *                             |              |
 *              +--------------+--------------+
 *              |              |              |
 *              |              |              |
 *       Server |       Client |   Time after |
 *       revoke |   deactivate |    "expires" |
 *              V              V              V
 *           revoked      deactivated      expired
 */
enum AuthorizationStatus:
  case pending, valid, invalid, deactivated, expired, revoked
end AuthorizationStatus
object AuthorizationStatus:
  given eqAuthorizationStatus: Eq[AuthorizationStatus] = Eq.fromUniversalEquals[AuthorizationStatus]
  given stringCodecAuthorizationStatus[F[_]: Applicative]: Codec[F, String, String, AuthorizationStatus] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, AuthorizationStatus](using CodecConfig.default)
  given encodeAuthorizationStatus[F[_]: Applicative, S: StringType]: Encoder[F, S, AuthorizationStatus] =
    Encoder.encodeS[F, S, AuthorizationStatus]
  given decodeAuthorizationStatus[F[_]: Applicative, S: {StringType, Show}]: Decoder[F, Cursor[S], AuthorizationStatus] =
    Decoder.decodeS[F, S, AuthorizationStatus]
  given showAuthorizationStatus: Show[AuthorizationStatus] = Show.fromToString[AuthorizationStatus]
end AuthorizationStatus
