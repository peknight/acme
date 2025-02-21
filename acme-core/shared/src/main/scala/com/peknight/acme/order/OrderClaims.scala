package com.peknight.acme.order

import cats.Monad
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}

import java.time.Instant

case class OrderClaims(
                        identifiers: List[Identifier],
                        notBefore: Option[Instant] = None,
                        notAfter: Option[Instant] = None,
                        autoRenewal: Option[AutoRenewal] = None,
                        replaces: Option[String] = None,
                        profile: Option[String] = None,
                      )
object OrderClaims:
  private val memberNameMap: Map[String, String] = Map(
    "autoRenewal" -> "auto-renewal"
  )
  given codecOrderClaims[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                    NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                    Decoder[F, Cursor[S], JsonObject]): Codec[F, S, Cursor[S], OrderClaims] =
    given CodecConfiguration = CodecConfiguration.default
      .withTransformMemberNames(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, OrderClaims]

  given jsonCodecOrderClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], OrderClaims] = codecOrderClaims[F, Json]

  given circeCodecOrderClaims: io.circe.Codec[OrderClaims] = codec[OrderClaims]
end OrderClaims
