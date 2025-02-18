package com.peknight.acme.account

import cats.Monad
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.jose.jwk.JsonWebKey
import io.circe.{Json, JsonObject}

import java.time.Instant

case class NewAccountResponse(
                               key: Option[JsonWebKey],
                               createdAt: Option[Instant],
                               status: Option[AccountStatus],
                               ext: JsonObject
                             ) extends Ext
object NewAccountResponse:
  given codecNewAccountResponse[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                         NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                         Decoder[F, Cursor[S], JsonObject]): Codec[F, S, Cursor[S], NewAccountResponse] =
    given CodecConfiguration = CodecConfiguration.default.withExtField("ext")
    Codec.derived[F, S, NewAccountResponse]

  given jsonCodecNewAccountResponse[F[_]: Monad]: Codec[F, Json, Cursor[Json], NewAccountResponse] =
    codecNewAccountResponse[F, Json]

  given circeCodecNewAccountResponse: io.circe.Codec[NewAccountResponse] = codec[NewAccountResponse]
end NewAccountResponse
