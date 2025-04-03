package com.peknight.acme.identifier

import cats.{Id, Monad}
import com.comcast.ip4s.IpAddress
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.ip4s.instances.host.stringCodecIpAddress
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.error.Error
import com.peknight.error.syntax.`try`.asError
import io.circe.{Json, JsonObject}

import java.net.IDN
import java.util.Locale
import scala.util.Try

sealed trait Identifier extends Ext:
  def `type`: IdentifierType
  def value: String
  def ancestorDomain: Option[String]
  def subdomainAuthAllowed: Option[Boolean]
end Identifier
object Identifier:
  case class DNS(value: String, ancestorDomain: Option[String] = None, subdomainAuthAllowed: Option[Boolean] = None,
                 ext: JsonObject = JsonObject.empty) extends Identifier:
    def `type`: IdentifierType = IdentifierType.dns
    def asciiValue: Either[Error, String] = toAscii(value)
  end DNS
  case class IP(value: String, ancestorDomain: Option[String] = None, subdomainAuthAllowed: Option[Boolean] = None,
                 ext: JsonObject = JsonObject.empty) extends Identifier:
    def `type`: IdentifierType = IdentifierType.ip
    def ipAddress: Either[Error, IpAddress] = stringCodecIpAddress[Id].decode(value)
  end IP
  case class Email(value: String, ancestorDomain: Option[String] = None, subdomainAuthAllowed: Option[Boolean] = None,
                ext: JsonObject = JsonObject.empty) extends Identifier:
    def `type`: IdentifierType = IdentifierType.email
  end Email
  case class TNAuthList(value: String, ancestorDomain: Option[String] = None,
                        subdomainAuthAllowed: Option[Boolean] = None, ext: JsonObject = JsonObject.empty)
    extends Identifier:
    def `type`: IdentifierType = IdentifierType.TNAuthList
  end TNAuthList
  case class Reserved(value: String, ancestorDomain: Option[String] = None,
                      subdomainAuthAllowed: Option[Boolean] = None,
                      ext: JsonObject = JsonObject.empty) extends Identifier:
    def `type`: IdentifierType = IdentifierType.RESERVED
  end Reserved

  private def toAscii(domain: String): Either[Error, String] =
    Try(IDN.toASCII(domain.trim).toLowerCase(Locale.ENGLISH)).asError

  def dns(domain: String): Either[Error, Identifier] =
    toAscii(domain).map(DNS(_))

  private val constructorNameMap: Map[String, String] = Map(
    "DNS" -> "dns",
    "IP" -> "ip",
    "Email" -> "email",
    "Reserved" -> "RESERVED"
  )

  private val codecConfiguration: CodecConfiguration =
    CodecConfiguration.default
      .withTransformConstructorName(constructorName => constructorNameMap.getOrElse(constructorName, constructorName))
      .withDiscriminator("type").withExtField("ext")

  given codecDNS[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                          StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], DNS] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, DNS]

  given codecIP[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                         StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], IP] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, IP]

  given codecEmail[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                         StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Email] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, Email]

  given codecTNAuthList[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                            StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], TNAuthList] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, TNAuthList]

  given codecReserved[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                 StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Reserved] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, Reserved]

  given codecIdentifier[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                 StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject]
                                ): Codec[F, S, Cursor[S], Identifier] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, Identifier]
  given jsonCodecIdentifier[F[_]: Monad]: Codec[F, Json, Cursor[Json], Identifier] =
    codecIdentifier[F, Json]
  given circeCodecIdentifier: io.circe.Codec[Identifier] = codec[Identifier]
end Identifier
