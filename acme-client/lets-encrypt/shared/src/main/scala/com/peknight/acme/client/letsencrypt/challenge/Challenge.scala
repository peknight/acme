package com.peknight.acme.client.letsencrypt.challenge

import cats.Monad
import com.peknight.acme.challenge.ChallengeStatus
import com.peknight.acme.validation.ValidationMethod
import com.peknight.codec.base.Base64UrlNoPad
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

import java.time.Instant

case class Challenge(
                      `type`: ValidationMethod,
                      url: Uri,
                      status: ChallengeStatus,
                      token: Base64UrlNoPad,
                      validated: Option[Instant] = None,
                      ext: JsonObject = JsonObject.empty
                    ) extends com.peknight.acme.challenge.Challenge with Ext
object Challenge:
  given codecChallenge[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                              StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Challenge] =
    given CodecConfiguration = CodecConfiguration.default.withExtField("ext")
    Codec.derived[F, S, Challenge]

  given jsonCodecChallenge[F[_]: Monad]: Codec[F, Json, Cursor[Json], Challenge] = codecChallenge[F, Json]

  given circeCodecChallenge: io.circe.Codec[Challenge] = codec[Challenge]
end Challenge
