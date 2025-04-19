package com.peknight.acme.client.letsencrypt.challenge

import cats.syntax.contravariant.*
import cats.{Monad, Show}
import com.comcast.ip4s.{Host, Hostname}
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.generic.derivation.show
import io.circe.{Json, JsonObject}

case class ValidationRecord(hostname: Option[Hostname] = None, ext: JsonObject = JsonObject.empty) extends Ext
object ValidationRecord:
  given codecValidationRecord[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                       NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                       Decoder[F, Cursor[S], JsonObject], Show[S])
  : Codec[F, S, Cursor[S], ValidationRecord] =
    given CodecConfig = CodecConfig.default.withExtField("ext")
    Codec.derived[F, S, ValidationRecord]

  given jsonCodecValidationRecord[F[_]: Monad]: Codec[F, Json, Cursor[Json], ValidationRecord] =
    codecValidationRecord[F, Json]

  given circeCodecValidationRecord: io.circe.Codec[ValidationRecord] = codec[ValidationRecord]
  given showValidationRecord: Show[ValidationRecord] =
    given Show[Hostname] = Show[Host].contramap[Hostname](identity)
    show.derived[ValidationRecord]
end ValidationRecord
