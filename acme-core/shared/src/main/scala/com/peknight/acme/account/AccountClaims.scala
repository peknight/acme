package com.peknight.acme.account

import cats.{Monad, Show}
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.generic.derivation.show
import com.peknight.jose.jws.JsonWebSignature
import io.circe.{Json, JsonObject}
import org.http4s.Uri

case class AccountClaims(
                          contact: Option[List[Uri]] = None,
                          termsOfServiceAgreed: Option[Boolean] = None,
                          onlyReturnExisting: Option[Boolean] = None,
                          externalAccountBinding: Option[JsonWebSignature] = None,
                          status: Option[AccountStatus] = None
                        )
object AccountClaims:
  given codecAccountClaims[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                    NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                    Decoder[F, Cursor[S], JsonObject], Show[S]): Codec[F, S, Cursor[S], AccountClaims] =
    Codec.derived[F, S, AccountClaims]

  given jsonCodecAccountClaims[F[_]: Monad]: Codec[F, Json, Cursor[Json], AccountClaims] = codecAccountClaims[F, Json]

  given circeCodecAccountClaims: io.circe.Codec[AccountClaims] = codec[AccountClaims]

  given showAccountClaims: Show[AccountClaims] = show.derived[AccountClaims]
end AccountClaims
