package com.peknight.acme

import cats.Monad
import com.peknight.codec.Decoder.decodeOptionAOU
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

case class Directory(
                      newNonce: Uri,
                      newAccount: Uri,
                      newOrder: Uri,
                      newAuthorization: Option[Uri],
                      revokeCertificate: Uri,
                      keyChange: Uri,
                      meta: Option[Meta] = None,
                      renewalInfo: Option[Uri] = None,
                      ext: JsonObject = JsonObject.empty
                    )
  extends Ext
object Directory:
  private val memberNameMap: Map[String, String] = Map(
    "newAuthorization" -> "newAuthz",
    "revokeCertificate" -> "revokeCert"
  )
  given codecDirectory[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Directory] =
    given CodecConfiguration = CodecConfiguration.default
      .withTransformMemberNames(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, Directory]
  given jsonCodecDirectory[F[_]: Monad]: Codec[F, Json, Cursor[Json], Directory] =
    codecDirectory[F, Json]
  given circeCodecDirectory: io.circe.Codec[Directory] = codec[Directory]
end Directory
