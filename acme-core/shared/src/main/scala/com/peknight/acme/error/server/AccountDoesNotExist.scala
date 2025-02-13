package com.peknight.acme.error.server

import cats.Monad
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.Ext
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.status.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.JsonObject
import org.http4s.Status

case class AccountDoesNotExist(detail: String,
                               status: Option[Status] = None,
                               override val identifier: Option[Identifier] = None,
                               override val subProblems: Option[List[ACMEServerError]] = None,
                               ext: JsonObject = JsonObject.empty
                              ) extends ACMEServerError with Ext:
  def typeLabel: String = "accountDoesNotExist"
  def description: String = "The request specified an account that does not exist"
end AccountDoesNotExist
object AccountDoesNotExist:
  given codecAccountDoesNotExist[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S],
                                          NumberType[S], StringType[S], Encoder[F, S, JsonObject],
                                          Decoder[F, Cursor[S], JsonObject]
                                         ): Codec[F, S, Cursor[S], AccountDoesNotExist] =
    given CodecConfiguration = ACMEServerError.codecConfiguration
    Codec.derived[F, S, AccountDoesNotExist]
end AccountDoesNotExist
