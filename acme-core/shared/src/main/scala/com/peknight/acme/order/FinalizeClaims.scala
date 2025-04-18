package com.peknight.acme.order

import cats.{Monad, Show}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}

case class FinalizeClaims(certificateSigningRequest: Base64UrlNoPad)
object FinalizeClaims:
  private val memberNameMap: Map[String, String] = Map(
    "certificateSigningRequest" -> "csr"
  )
  given codecFinalizeClaims[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]
  : Codec[F, S, Cursor[S], FinalizeClaims] =
    given CodecConfig = CodecConfig.default
      .withTransformMemberName(memberName => memberNameMap.getOrElse(memberName, memberName))
    Codec.derived[F, S, FinalizeClaims]

  given jsonCodecFinalizeClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], FinalizeClaims] = codecFinalizeClaims[F, Json]

  given circeCodecFinalizeClaims: io.circe.Codec[FinalizeClaims] = codec[FinalizeClaims]
end FinalizeClaims
