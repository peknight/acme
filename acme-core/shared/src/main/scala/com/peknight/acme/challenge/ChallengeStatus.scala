package com.peknight.acme.challenge

import cats.{Applicative, Eq, Show}
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType
import com.peknight.codec.{Codec, Decoder, Encoder}

/**
 * Challenge objects are created in the "pending" state.  They
 * transition to the "processing" state when the client responds to the
 * challenge (see Section 7.5.1) and the server begins attempting to
 * validate that the client has completed the challenge.  Note that
 * within the "processing" state, the server may attempt to validate the
 * challenge multiple times (see Section 8.2).  Likewise, client
 * requests for retries do not cause a state change.  If validation is
 * successful, the challenge moves to the "valid" state; if there is an
 * error, the challenge moves to the "invalid" state.
 *
 *          pending
 *             |
 *             | Receive
 *             | response
 *             V
 *         processing <-+
 *             |   |    | Server retry or
 *             |   |    | client retry request
 *             |   +----+
 *             |
 *             |
 * Successful  |   Failed
 * validation  |   validation
 *   +---------+---------+
 *   |                   |
 *   V                   V
 * valid              invalid
 */
enum ChallengeStatus:
  case pending, processing, valid, invalid
end ChallengeStatus
object ChallengeStatus:
  given eqChallengeStatus: Eq[ChallengeStatus] = Eq.fromUniversalEquals[ChallengeStatus]
  given stringCodecChallengeStatus[F[_]: Applicative]: Codec[F, String, String, ChallengeStatus] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, ChallengeStatus](using CodecConfig.default)
  given encodeChallengeStatus[F[_]: Applicative, S: StringType]: Encoder[F, S, ChallengeStatus] =
    Encoder.encodeS[F, S, ChallengeStatus]
  given decodeChallengeStatus[F[_]: Applicative, S: {StringType, Show}]: Decoder[F, Cursor[S], ChallengeStatus] =
    Decoder.decodeS[F, S, ChallengeStatus]
  given showChallengeStatus: Show[ChallengeStatus] = Show.fromToString[ChallengeStatus]  
end ChallengeStatus
