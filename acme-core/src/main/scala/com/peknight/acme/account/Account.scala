package com.peknight.acme.account

import cats.{Monad, Show}
import com.peknight.cats.instances.instant.given
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.generic.derivation.show
import com.peknight.jose.jwk.JsonWebKey
import com.peknight.jose.jws.JsonWebSignature
import io.circe.{Json, JsonObject}
import org.http4s.Uri

import java.time.Instant

case class Account(
                    status: AccountStatus,
                    orders: Option[Uri],
                    contact: Option[List[Uri]] = None,
                    termsOfServiceAgreed: Option[Boolean] = None,
                    onlyReturnExisting: Option[Boolean] = None,
                    externalAccountBinding: Option[JsonWebSignature] = None,
                    // RFC9115
                    delegations: Option[Uri] = None,
                    key: Option[JsonWebKey] = None,
                    createdAt: Option[Instant] = None,
                    ext: JsonObject = JsonObject.empty
                  ) extends Ext
object Account:
  given codecAccount[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                              StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject], Show[S])
  : Codec[F, S, Cursor[S], Account] =
    given CodecConfig = CodecConfig.default.withExtField("ext")
    Codec.derived[F, S, Account]

  given jsonCodecAccount[F[_]: Monad]: Codec[F, Json, Cursor[Json], Account] = codecAccount[F, Json]

  given circeCodecAccount: io.circe.Codec[Account] = codec[Account]

  given showAccount: Show[Account] = show.derived[Account]
end Account
