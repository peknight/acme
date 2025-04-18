package com.peknight.acme.directory

import cats.{Monad, Show}
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
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
                      revokeCertificate: Uri,
                      keyChange: Uri,
                      newAuthorization: Option[Uri] = None,
                      meta: Option[Meta] = None,
                      renewalInfo: Option[Uri] = None,
                      ext: JsonObject = JsonObject.empty
                    ) extends Ext
object Directory:
  private val memberNameMap: Map[String, String] = Map(
    "newAuthorization" -> "newAuthz",
    "revokeCertificate" -> "revokeCert"
  )
  given codecDirectory[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject], Show[S])
  : Codec[F, S, Cursor[S], Directory] =
    given CodecConfig = CodecConfig.default
      .withTransformMemberName(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, Directory]
  given jsonCodecDirectory[F[_]: Monad]: Codec[F, Json, Cursor[Json], Directory] =
    codecDirectory[F, Json]
  given circeCodecDirectory: io.circe.Codec[Directory] = codec[Directory]
end Directory
