package com.peknight.acme.authorization

import cats.{Monad, Show}
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}

case class PreAuthorizationClaims(identifier: Identifier)
object PreAuthorizationClaims:
  given codecPreAuthorizationClaims[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                             NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                             Decoder[F, Cursor[S], JsonObject], Show[S])
  : Codec[F, S, Cursor[S], PreAuthorizationClaims] =
    Codec.derived[F, S, PreAuthorizationClaims]

  given jsonCodecPreAuthorizationClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], PreAuthorizationClaims] =
    codecPreAuthorizationClaims[F, Json]

  given circeCodecPreAuthorizationClaims: io.circe.Codec[PreAuthorizationClaims] = codec[PreAuthorizationClaims]
end PreAuthorizationClaims
