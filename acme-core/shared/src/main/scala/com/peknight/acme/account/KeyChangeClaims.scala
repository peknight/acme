package com.peknight.acme.account

import cats.{Monad, Show}
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.jose.jwk.JsonWebKey
import io.circe.{Json, JsonObject}
import org.http4s.Uri

case class KeyChangeClaims(account: Uri, oldKey: JsonWebKey)
object KeyChangeClaims:
  given codecKeyChangeClaims[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                      NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                      Decoder[F, Cursor[S], JsonObject], Show[S]): Codec[F, S, Cursor[S], KeyChangeClaims] =
    Codec.derived[F, S, KeyChangeClaims]

  given jsonCodecKeyChangeClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], KeyChangeClaims] =
    codecKeyChangeClaims[F, Json]

  given circeCodecKeyChangeClaims: io.circe.Codec[KeyChangeClaims] = codec[KeyChangeClaims]
end KeyChangeClaims
