package com.peknight.acme.identifier

import cats.Monad
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.error.Error
import com.peknight.error.syntax.`try`.asError
import io.circe.{Json, JsonObject}

import java.net.IDN
import java.util.Locale
import scala.util.Try

case class Identifier(
                       `type`: IdentifierType,
                       value: String,
                       ancestorDomain: Option[String] = None,
                       subdomainAuthAllowed: Option[Boolean] = None,
                       ext: JsonObject = JsonObject.empty
                     ) extends Ext
object Identifier:
  def dns(domain: String): Either[Error, Identifier] =
    Try(IDN.toASCII(domain.trim).toLowerCase(Locale.ENGLISH))
      .asError
      .map(domain => Identifier(IdentifierType.dns, domain))
  given codecIdentifier[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                 StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject]
                                ): Codec[F, S, Cursor[S], Identifier] =
    given CodecConfiguration = CodecConfiguration.default.withExtField("ext")
    Codec.derived[F, S, Identifier]
  given jsonCodecIdentifier[F[_]: Monad]: Codec[F, Json, Cursor[Json], Identifier] =
    codecIdentifier[F, Json]
  given circeCodecIdentifier: io.circe.Codec[Identifier] = codec[Identifier]
end Identifier
