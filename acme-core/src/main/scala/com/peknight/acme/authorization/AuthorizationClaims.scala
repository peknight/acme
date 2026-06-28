package com.peknight.acme.authorization

import cats.{Monad, Show}
import com.peknight.codec.Codec
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import io.circe.Json

case class AuthorizationClaims(status: Option[AuthorizationStatus] = None)
object AuthorizationClaims:
  given codecAuthorizationClaims[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]
  : Codec[F, S, Cursor[S], AuthorizationClaims] =
    Codec.derived[F, S, AuthorizationClaims]

  given jsonCodecAuthorizationClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], AuthorizationClaims] =
    codecAuthorizationClaims[F, Json]

  given circeCodecAuthorizationClaims: io.circe.Codec[AuthorizationClaims] = codec[AuthorizationClaims]
end AuthorizationClaims
