package com.peknight.acme.directory

import cats.Monad
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.instances.time.duration.codecDurationOfSecondsNS
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.commons.text.cases.KebabCase
import com.peknight.commons.text.syntax.cases.to
import io.circe.{Json, JsonObject}

import scala.concurrent.duration.FiniteDuration

/**
 * RFC8739
 */
case class AutoRenewal(
                        // Minimum acceptable value for auto-renewal lifetime, in seconds.
                        minLifetime: Option[FiniteDuration] = None,
                        // Maximum allowed delta between the end-date and start-date attributes of the Order's
                        // auto-renewal object.
                        maxDuration: Option[FiniteDuration] = None,
                        // If this field is present and set to "true", the server allows GET (and HEAD) requests to
                        // star-certificate URLs.
                        // If this field is present and set to "true", the client requests the server to allow
                        // unauthenticated GET (and HEAD) to the star-certificate associated with this Order.
                        allowCertificateGet: Option[Boolean] = None,
                        ext: JsonObject = JsonObject.empty
                      ) extends Ext
object AutoRenewal:
  given codecAutoRenewal[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                  NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                  Decoder[F, Cursor[S], JsonObject]): Codec[F, S, Cursor[S], AutoRenewal] =
    given CodecConfiguration = CodecConfiguration.default
      .withTransformMemberName(_.to(KebabCase))
      .withExtField("ext")
    given Codec[F, S, Cursor[S], FiniteDuration] = codecDurationOfSecondsNS
    Codec.derived[F, S, AutoRenewal]
  given jsonCodecAutoRenewal[F[_]: Monad]: Codec[F, Json, Cursor[Json], AutoRenewal] =
    codecAutoRenewal[F, Json]
  given circeCodecAutoRenewal: io.circe.Codec[AutoRenewal] = codec[AutoRenewal]
end AutoRenewal
