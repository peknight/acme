package com.peknight.acme.jose

import cats.{Monad, Show}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.Json
import org.http4s.Uri

case class JWSHeaderExt(url: Uri, nonce: Option[Base64UrlNoPad] = None)
object JWSHeaderExt:
  given codecJWSHeaderExt[F[_]: Monad, S: {ObjectType, NullType, StringType, Show}]
  : Codec[F, S, Cursor[S], JWSHeaderExt] =
    Codec.derived[F, S, JWSHeaderExt]
  given jsonCodecJWSHeaderExt[F[_]: Monad]: Codec[F, Json, Cursor[Json], JWSHeaderExt] =
    codecJWSHeaderExt[F, Json]
  given circeCodecJWSHeaderExt: io.circe.Codec[JWSHeaderExt] = codec[JWSHeaderExt]
end JWSHeaderExt
