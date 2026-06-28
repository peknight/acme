package com.peknight.acme.error

import cats.syntax.eq.*
import cats.{Monad, Show}
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.status.given
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import com.peknight.error.Error
import com.peknight.generic.derivation.show
import com.peknight.http.error.Problem
import io.circe.{Json, JsonObject}
import org.http4s.{Status, Uri}

case class ACMEError(
                      `type`: Uri,
                      title: Option[String] = None,
                      status: Option[Status] = None,
                      detail: Option[String] = None,
                      instance: Option[Uri] = None,
                      identifier: Option[Identifier] = None,
                      subProblems: Option[List[ACMEError]] = None,
                      ext: JsonObject = JsonObject.empty
                    ) extends Problem:
  def acmeErrorType: Option[ACMEErrorType] = ACMEErrorType.values.find(_.`type` === `type`)
  override def labelOption: Option[String] = title
  override def lowPriorityMessage: Option[String] = detail
  override def cause: Option[Error] = subProblems.map(Error.apply)
end ACMEError
object ACMEError:
  private val memberNameMap: Map[String, String] = Map(
    "subProblems" -> "subproblems"
  )
  given codecACMEError[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject], Show[S])
  : Codec[F, S, Cursor[S], ACMEError] =
    given CodecConfig = CodecConfig.default
      .withTransformMemberName(memberName => memberNameMap.getOrElse(memberName, memberName))
      .withExtField("ext")
    Codec.derived[F, S, ACMEError]
  given jsonCodecACMEError[F[_]: Monad]: Codec[F, Json, Cursor[Json], ACMEError] =
    codecACMEError[F, Json]
  given circeCodecACMEError: io.circe.Codec[ACMEError] = codec[ACMEError]
  given showACMEError: Show[ACMEError] = show.derived[ACMEError]
end ACMEError
