package com.peknight.acme.directory

import cats.Monad
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}
import org.http4s.Uri

case class Profiles(classic: String, tlsServer: Uri, ext: JsonObject = JsonObject.empty) extends Ext
object Profiles:
  private val memberNameMap: Map[String, String] = Map(
    "tlsServer" -> "tlsserver"
  )
  given codecProfiles[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                               StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Profiles] =
    given CodecConfiguration = CodecConfiguration.default
      .withTransformMemberNames(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, Profiles]
  given jsonCodecProfiles[F[_]: Monad]: Codec[F, Json, Cursor[Json], Profiles] =
    codecProfiles[F, Json]
  given circeCodecProfiles: io.circe.Codec[Profiles] = codec[Profiles]
end Profiles
