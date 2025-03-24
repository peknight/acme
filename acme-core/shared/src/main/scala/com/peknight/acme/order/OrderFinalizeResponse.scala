package com.peknight.acme.order

import cats.Monad
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}

case class OrderFinalizeResponse(ext: JsonObject = JsonObject.empty) extends Ext

object OrderFinalizeResponse:
  given codecOrderFinalizeResponse[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                            NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                            Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], OrderFinalizeResponse] =
    given CodecConfiguration = CodecConfiguration.default.withExtField("ext")
    Codec.derived[F, S, OrderFinalizeResponse]
  given jsonCodecOrderFinalizeResponse[F[_]: Monad]: Codec[F, Json, Cursor[Json], OrderFinalizeResponse] =
    codecOrderFinalizeResponse[F, Json]
  given circeCodecOrderFinalizeResponse: io.circe.Codec[OrderFinalizeResponse] = codec[OrderFinalizeResponse]
end OrderFinalizeResponse
