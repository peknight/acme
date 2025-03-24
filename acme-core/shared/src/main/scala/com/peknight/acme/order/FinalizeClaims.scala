package com.peknight.acme.order

import cats.Monad
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}

case class FinalizeClaims(certificateSigningRequest: Base64UrlNoPad)
object FinalizeClaims:
  private val memberNameMap: Map[String, String] = Map(
    "certificateSigningRequest" -> "csr"
  )
  given codecFinalizeClaims[F[_], S](using Monad[F], ObjectType[S], NullType[S], StringType[S])
  : Codec[F, S, Cursor[S], FinalizeClaims] =
    given CodecConfiguration = CodecConfiguration.default
      .withTransformMemberNames(memberName => memberNameMap.getOrElse(memberName, memberName))
    Codec.derived[F, S, FinalizeClaims]

  given jsonCodecFinalizeClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], FinalizeClaims] = codecFinalizeClaims[F, Json]

  given circeCodecFinalizeClaims: io.circe.Codec[FinalizeClaims] = codec[FinalizeClaims]
end FinalizeClaims
