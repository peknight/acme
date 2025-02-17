package com.peknight.acme.error

import cats.Monad
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.http4s.instances.status.given
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.error.NoSuchEnum
import com.peknight.codec.sum.{ArrayType, NullType, NumberType, ObjectType, StringType}
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.error.Error
import io.circe.Json
import org.http4s.Status

case class ACMEError(
                      acmeErrorType: ACMEErrorType,
                      detail: String,
                      identifier: Option[Identifier] = None,
                      subProblems: Option[List[ACMEError]] = None,
                      status: Option[Status] = None,
                    ) extends Error:
  def `type`: String = ACMEError.`type`(acmeErrorType)
  override def message: String = detail
  override def cause: Option[Error] = subProblems.map(Error.apply)
end ACMEError
object ACMEError:
  def `type`(acmeErrorType: ACMEErrorType): String = s"urn:ietf:params:acme:error:$acmeErrorType"

  given codecACMEError[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], NumberType[S], StringType[S])
  : Codec[F, S, Cursor[S], ACMEError] =
    Codec.forProduct[F, S, ACMEError, (String, String, Option[Identifier], Option[List[ACMEError]], Option[Status])]
      (("type", "detail", "identifier", "subproblems", "status"))(acmeError =>
        (acmeError.`type`, acmeError.detail, acmeError.identifier, acmeError.subProblems, acmeError.status)
      ) { (typ, detail, identifier, subProblems, status) =>
        ACMEErrorType.values.find(errorType => `type`(errorType) == typ)
          .map(errorType => ACMEError(errorType, detail, identifier, subProblems, status))
          .toRight(NoSuchEnum(typ))
      }
  given jsonCodecACMEError[F[_]: Monad]: Codec[F, Json, Cursor[Json], ACMEError] =
    codecACMEError[F, Json]
  given circeCodecACMEError: io.circe.Codec[ACMEError] = codec[ACMEError]
end ACMEError
