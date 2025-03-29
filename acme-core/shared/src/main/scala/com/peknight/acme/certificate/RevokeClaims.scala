package com.peknight.acme.certificate

import cats.Monad
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.security.certificate.revocation.list.ReasonCode
import com.peknight.security.codec.instances.certificate.revocation.list.reasonCode.given
import io.circe.Json

case class RevokeClaims(certificate: Base64UrlNoPad, reason: Option[ReasonCode])
object RevokeClaims:
  given codecRevokeClaims[F[_], S](using Monad[F], ObjectType[S], NullType[S], NumberType[S], StringType[S])
  : Codec[F, S, Cursor[S], RevokeClaims] =
    Codec.derived[F, S, RevokeClaims]

  given jsonCodecRevokeClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], RevokeClaims] = codecRevokeClaims[F, Json]

  given circeCodecRevokeClaims: io.circe.Codec[RevokeClaims] = codec[RevokeClaims]
end RevokeClaims
