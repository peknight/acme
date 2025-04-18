package com.peknight.acme.order

import cats.data.NonEmptyList
import com.peknight.cats.instances.time.instant.given
import cats.{Monad, Show}
import com.peknight.acme.error.ACMEError
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.generic.derivation.show
import io.circe.{Json, JsonObject}
import org.http4s.Uri

import java.time.Instant

case class Order(
                  status: OrderStatus,
                  identifiers: NonEmptyList[Identifier],
                  authorizations: List[Uri],
                  finalizeUri: Uri,
                  expires: Option[Instant] = None,
                  notBefore: Option[Instant] = None,
                  notAfter: Option[Instant] = None,
                  error: Option[ACMEError] = None,
                  certificate: Option[Uri] = None,
                  autoRenewal: Option[AutoRenewal] = None,
                  starCertificate: Option[Uri] = None,
                  allowCertificateGet: Option[Boolean] = None,
                  delegation: Option[Uri] = None,
                  replaces: Option[String] = None,
                  profile: Option[String] = None,
                  ext: JsonObject = JsonObject.empty
                ) extends Ext with OrderPlatform
object Order:
  private val memberNameMap: Map[String, String] = Map(
    "finalizeUri" -> "finalize",
    "autoRenewal" -> "auto-renewal",
    "starCertificate" -> "star-certificate",
    "allowCertificateGet" -> "allow-certificate-get"
  )
  given codecOrder[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                            StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject], Show[S])
  : Codec[F, S, Cursor[S], Order] =
    given CodecConfig = CodecConfig.default
      .withTransformMemberName(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, Order]
  given jsonCodecOrder[F[_]: Monad]: Codec[F, Json, Cursor[Json], Order] =
    codecOrder[F, Json]
  given circeCodecOrder: io.circe.Codec[Order] = codec[Order]
  given showOrder: Show[Order] = show.derived[Order]
end Order
