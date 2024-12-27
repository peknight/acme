package com.peknight.acme

import cats.Monad
import com.comcast.ip4s.Host
import com.peknight.codec.Codec
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.sum.*
import io.circe.Json
import org.http4s.Uri

case class Meta(
                 termsOfService: Option[Uri] = None,
                 website: Option[Uri] = None,
                 caaIdentities: Option[List[Host]] = None,
                 externalAccountRequired: Option[Boolean] = None
               )
object Meta:
  given codecMeta[F[_]: Monad, S: ObjectType: NullType: ArrayType: BooleanType: StringType]
  : Codec[F, S, Cursor[S], Meta] =
    Codec.derived[F, S, Meta]
  given jsonCodecMeta[F[_]: Monad]: Codec[F, Json, Cursor[Json], Meta] =
    codecMeta[F, Json]
  given circeCodecMeta: io.circe.Codec[Meta] = codec[Meta]
end Meta
