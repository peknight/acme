package com.peknight.acme.identifier

import cats.Monad
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.{NullType, ObjectType, StringType}
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.Json

case class Identifier(`type`: IdentifierType, value: String)
object Identifier:
  given codecIdentifier[F[_], S](using Monad[F], ObjectType[S], NullType[S], StringType[S]): Codec[F, S, Cursor[S], Identifier] =
    Codec.derived[F, S, Identifier]
  given jsonCodecIdentifier[F[_]: Monad]: Codec[F, Json, Cursor[Json], Identifier] =
    codecIdentifier[F, Json]
  given circeCodecIdentifier: io.circe.Codec[Identifier] = codec[Identifier]
end Identifier
