package com.peknight.acme.authorization

import cats.data.NonEmptyList
import cats.{Id, Monad, Show}
import com.peknight.acme.identifier.Identifier
import com.peknight.cats.instances.time.instant.given
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.generic.derivation.show
import io.circe.{Json, JsonObject}

import java.time.Instant

case class Authorization[Challenge](
                                     identifier: Identifier,
                                     status: AuthorizationStatus,
                                     challenges: NonEmptyList[Challenge],
                                     expires: Option[Instant] = None,
                                     wildcard: Option[Boolean] = None,
                                     subdomainAuthAllowed: Option[Boolean] = None,
                                     ext: JsonObject = JsonObject.empty
                                   ) extends Ext
object Authorization:
  given encodeAuthorization[F[_], S, Challenge](using Monad[F], ObjectType[S], NullType[S], ArrayType[S],
                                                BooleanType[S], NumberType[S], StringType[S], Encoder[F, S, Challenge],
                                                Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject], Show[S])
  : Encoder[F, S, Authorization[Challenge]] =
    given CodecConfig = CodecConfig.default.withExtField("ext")
    Encoder.derived[F, S, Authorization[Challenge]]

  given decodeAuthorization[F[_], S, Challenge](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                                NumberType[S], StringType[S], Decoder[F, Cursor[S], Challenge],
                                                Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject], Show[S])
  : Decoder[F, Cursor[S], Authorization[Challenge]] =
    given CodecConfig = CodecConfig.default.withExtField("ext")
    Decoder.derived[F, S, Authorization[Challenge]]

  given jsonEncodeAuthorization[F[_], Challenge](using Monad[F], Encoder[F, Json, Challenge])
  : Encoder[F, Json, Authorization[Challenge]] =
    encodeAuthorization[F, Json, Challenge]

  given jsonDecodeAuthorization[F[_], Challenge](using Monad[F], Decoder[F, Cursor[Json], Challenge])
  : Decoder[F, Cursor[Json], Authorization[Challenge]] =
    decodeAuthorization[F, Json, Challenge]

  given circeCodecAuthorization[Challenge](using Encoder[Id, Json, Challenge], Decoder[Id, Cursor[Json], Challenge])
  : io.circe.Codec[Authorization[Challenge]] =
    codec[Authorization[Challenge]]

  given showAuthorization[Challenge](using Show[Challenge]): Show[Authorization[Challenge]] =
    show.derived[Authorization[Challenge]]
end Authorization
