package com.peknight.acme.order

import cats.{Monad, Show}
import com.peknight.cats.instances.instant.given
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.instances.time.finiteDuration.codecFiniteDurationOfSecondsNS
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.commons.text.cases.KebabCase
import com.peknight.commons.text.syntax.cases.to
import com.peknight.generic.derivation.show
import io.circe.{Json, JsonObject}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
 * RFC8739
 */
case class AutoRenewal(
                        startDate: Option[Instant] = None,
                        endDate: Option[Instant] = None,
                        lifetime: Option[FiniteDuration] = None,
                        lifetimeAdjust: Option[FiniteDuration] = None,
                        allowCertificateGet: Option[Boolean] = None,
                        ext: JsonObject = JsonObject.empty
                      ) extends Ext
object AutoRenewal:
  given codecAutoRenewal[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                  NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                  Decoder[F, Cursor[S], JsonObject], Show[S]): Codec[F, S, Cursor[S], AutoRenewal] =
    given CodecConfig = CodecConfig.default
      .withTransformMemberName(_.to(KebabCase))
      .withExtField("ext")
    given Codec[F, S, Cursor[S], FiniteDuration] = codecFiniteDurationOfSecondsNS
    Codec.derived[F, S, AutoRenewal]
  given jsonCodecAutoRenewal[F[_]: Monad]: Codec[F, Json, Cursor[Json], AutoRenewal] =
    codecAutoRenewal[F, Json]
  given circeCodecAutoRenewal: io.circe.Codec[AutoRenewal] = codec[AutoRenewal]
  given showAutoRenewal: Show[AutoRenewal] = show.derived[AutoRenewal]
end AutoRenewal
