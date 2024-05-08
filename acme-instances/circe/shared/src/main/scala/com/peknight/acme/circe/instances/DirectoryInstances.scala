package com.peknight.acme.circe.instances

import cats.Monad
import com.peknight.acme.{Directory, Meta}
import com.peknight.codec.Codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.ip4s.instances.host.given
import io.circe.Json

trait DirectoryInstances:
  given codecMeta[F[_]](using CodecConfiguration, Monad[F]): Codec[F, Json, Cursor[Json], Meta] =
    Codec.derived[F, Json, Meta]

  given codecDirectory[F[_]](using CodecConfiguration, Monad[F]): Codec[F, Json, Cursor[Json], Directory] =
    Codec.derived[F, Json, Directory]
end DirectoryInstances
