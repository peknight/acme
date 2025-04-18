package com.peknight.acme.order

import cats.data.NonEmptyList
import cats.{Monad, Show}
import com.peknight.acme.identifier.Identifier
import com.peknight.cats.instances.time.instant.given
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.generic.derivation.show
import io.circe.{Json, JsonObject}

import java.time.Instant

case class OrderClaims(
                        identifiers: NonEmptyList[Identifier],
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
                                  Decoder[F, Cursor[S], JsonObject], Show[S]): Codec[F, S, Cursor[S], OrderClaims] =
    given CodecConfig = CodecConfig.default
      .withTransformMemberName(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, OrderClaims]

  given jsonCodecOrderClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], OrderClaims] = codecOrderClaims[F, Json]

  given circeCodecOrderClaims: io.circe.Codec[OrderClaims] = codec[OrderClaims]

  given showOrderClaims: Show[OrderClaims] = show.derived[OrderClaims]
end OrderClaims
