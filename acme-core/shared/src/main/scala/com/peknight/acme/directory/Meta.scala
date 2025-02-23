package com.peknight.acme.directory

import cats.{Id, Monad}
import com.comcast.ip4s.Host
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.syntax.encoder.asS
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}
import org.http4s.Uri

case class Meta(
                 termsOfService: Option[Uri] = None,
                 website: Option[Uri] = None,
                 caaIdentities: Option[List[Host]] = None,
                 externalAccountRequired: Option[Boolean] = None,
                 autoRenewal: Option[AutoRenewal] = None,
                 delegationEnabled: Option[Boolean] = None,
                 allowCertificateGet: Option[Boolean] = None,
                 // Whether the CA supports subdomain auth according to RFC9444.
                 subdomainAuthAllowed: Option[Boolean] = None,
                 // RFC-ietf-acme-onion-07
                 onionCAARequired: Option[Boolean] = None,
                 profiles: Option[Profiles] = None,
                 ext: JsonObject = JsonObject.empty
               ) extends Ext:
  def autoRenewalEnabled: Boolean = autoRenewal.isDefined
  def autoRenewalGetAllowed: Boolean = autoRenewal.flatMap(_.allowCertificateGet).getOrElse(false)
  def profileAllowed: Boolean = profiles.isDefined
  def profileAllowed(profile: String): Boolean = 
    profiles.flatMap(_.asS[Id, Json].asObject).flatMap(_(profile)).isDefined
end Meta
object Meta:
  private val memberNameMap: Map[String, String] = Map(
    "autoRenewal" -> "auto-renewal",
    "delegationEnabled" -> "delegation-enabled",
    "allowCertificateGet" -> "allow-certificate-get"
  )
  given codecMeta[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                           StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Meta] =
    given CodecConfiguration = CodecConfiguration.default
      .withTransformMemberNames(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, Meta]
  given jsonCodecMeta[F[_]: Monad]: Codec[F, Json, Cursor[Json], Meta] =
    codecMeta[F, Json]
  given circeCodecMeta: io.circe.Codec[Meta] = codec[Meta]
end Meta
