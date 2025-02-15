package com.peknight.acme.error

import cats.Monad
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.NoSuchEnum
import com.peknight.codec.sum.{ArrayType, NullType, ObjectType, StringType}
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.error.Error
import io.circe.Json

case class ACMEError(
                      acmeErrorType: ACMEErrorType,
                      detail: String,
                      identifier: Option[Identifier] = None,
                      subProblems: Option[List[ACMEError]] = None
                    ) extends Error:
  def `type`: String = ACMEError.`type`(acmeErrorType)
  override def message: String = detail
  override def cause: Option[Error] = subProblems.map(Error.apply)
end ACMEError
object ACMEError:
  def `type`(acmeErrorType: ACMEErrorType): String = s"urn:ietf:params:acme:error:$acmeErrorType"

  given codecACMEError[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], StringType[S])
  : Codec[F, S, Cursor[S], ACMEError] =
    Codec.forProduct[F, S, ACMEError, (String, String, Option[Identifier], Option[List[ACMEError]])]
      (("type", "detail", "identifier", "subproblems"))(acmeError =>
        (acmeError.`type`, acmeError.detail, acmeError.identifier, acmeError.subProblems)
      ) { (typ, detail, identifier, subProblems) =>
        ACMEErrorType.values.find(errorType => `type`(errorType) == typ)
          .map(errorType => ACMEError(errorType, detail, identifier, subProblems))
          .toRight(NoSuchEnum(typ))
      }
  given jsonCodecACMEError[F[_]: Monad]: Codec[F, Json, Cursor[Json], ACMEError] =
    codecACMEError[F, Json]
  given circeCodecACMEError: io.circe.Codec[ACMEError] = codec[ACMEError]
end ACMEError
