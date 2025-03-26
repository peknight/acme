package com.peknight.acme.account

import cats.Monad
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
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
                              StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Account] =
    given CodecConfiguration = CodecConfiguration.default.withExtField("ext")
    Codec.derived[F, S, Account]

  given jsonCodecAccount[F[_]: Monad]: Codec[F, Json, Cursor[Json], Account] = codecAccount[F, Json]

  given circeCodecAccount: io.circe.Codec[Account] = codec[Account]
end Account
