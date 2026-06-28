package com.peknight.acme.challenge

import cats.{Monad, Show}
import com.peknight.codec.Codec
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import io.circe.Json

case class ChallengeClaims()
object ChallengeClaims:
  given codecChallengeClaims[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]
  : Codec[F, S, Cursor[S], ChallengeClaims] =
    Codec.derived[F, S, ChallengeClaims]

  given jsonCodecChallengeClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], ChallengeClaims] =
    codecChallengeClaims[F, Json]

  given circeCodecChallengeClaims: io.circe.Codec[ChallengeClaims] = codec[ChallengeClaims]
end ChallengeClaims
