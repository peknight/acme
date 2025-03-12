package com.peknight.acme.client.letsencrypt.challenge

import cats.Monad
import cats.syntax.functor.*
import com.peknight.acme.challenge.ChallengeStatus
import com.peknight.acme.error.ACMEError
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.validation.ValidationMethod
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.circe.Ext
import com.peknight.codec.circe.iso.codec
import com.peknight.codec.circe.sum.jsonType.given
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.sum.*
import com.peknight.codec.{Codec, Decoder, Encoder}
import io.circe.{Json, JsonObject}
import org.http4s.Uri

import java.time.Instant

sealed trait Challenge extends com.peknight.acme.challenge.Challenge with Ext:
  def token: Base64UrlNoPad
  def validated: Option[Instant]
  def error: Option[ACMEError]
  def validationRecord: Option[List[ValidationRecord]]
end Challenge
object Challenge:

  case class `http-01`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad, validated: Option[Instant] = None,
                       validationRecord: Option[List[ValidationRecord]] = None, error: Option[ACMEError] = None, ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`http-01` with HTTP01Platform:
    def name: String = token.value
    def url(identifier: DNS): Uri = Uri.unsafeFromString(s"http://${identifier.value}/.well-known/acme-challenge/$name")
  end `http-01`
  case class `dns-01`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad, validated: Option[Instant] = None,
                      validationRecord: Option[List[ValidationRecord]] = None, error: Option[ACMEError] = None, ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`dns-01` with DNS01Platform:
    private def toRRName(domain: String): String = s"_acme-challenge.$domain."
    def name(identifier: DNS): String = toRRName(identifier.value)
  end `dns-01`
  case class `tls-sni-01`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad, validated: Option[Instant] = None,
                          validationRecord: Option[List[ValidationRecord]] = None, error: Option[ACMEError] = None,
                          ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`tls-sni-01`
  case class `tls-sni-02`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad, validated: Option[Instant] = None,
                          validationRecord: Option[List[ValidationRecord]] = None, error: Option[ACMEError] = None,
                          ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`tls-sni-02`
  case class `tls-alpn-01`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad, validated: Option[Instant] = None,
                           validationRecord: Option[List[ValidationRecord]] = None, error: Option[ACMEError] = None,
                           ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`tls-alpn-01`
  case class `email-reply-00`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad,
                              validated: Option[Instant] = None, validationRecord: Option[List[ValidationRecord]] = None,
                              error: Option[ACMEError] = None, ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`email-reply-00`
  case class `tkauth-01`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad, validated: Option[Instant] = None,
                         validationRecord: Option[List[ValidationRecord]] = None, error: Option[ACMEError] = None, ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`tkauth-01`
  case class `onion-csr-01`(url: Uri, status: ChallengeStatus, token: Base64UrlNoPad, validated: Option[Instant] = None,
                            validationRecord: Option[List[ValidationRecord]] = None, error: Option[ACMEError] = None, ext: JsonObject = JsonObject.empty)
    extends Challenge with com.peknight.acme.challenge.Challenge.`onion-csr-01`

  private val codecConfiguration: CodecConfiguration =
    CodecConfiguration.default.withDiscriminator("type").withExtField("ext")

  given codecHttp01[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                             StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `http-01`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `http-01`]

  given codecDns01[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                            StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `dns-01`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `dns-01`]

  given codecTlsSni01[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                               StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `tls-sni-01`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `tls-sni-01`]

  given codecTlsSni02[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                               StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `tls-sni-02`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `tls-sni-02`]

  given codecTlsAlpn01[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `tls-alpn-01`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `tls-alpn-01`]

  given codecEmailReply00[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                   StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `email-reply-00`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `email-reply-00`]

  given codecTkauth0101[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                 StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `tkauth-01`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `tkauth-01`]

  given codecOnionCsr01[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                 StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], `onion-csr-01`] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, `onion-csr-01`]

  given codecChallenge[F[_], S](using Monad[F], ObjectType[S], NullType[S], ArrayType[S], BooleanType[S], NumberType[S],
                                StringType[S], Encoder[F, S, JsonObject], Decoder[F, Cursor[S], JsonObject])
  : Codec[F, S, Cursor[S], Challenge] =
    given CodecConfiguration = codecConfiguration
    Codec.derived[F, S, Challenge]

  given jsonCodecChallenge[F[_]: Monad]: Codec[F, Json, Cursor[Json], Challenge] = codecChallenge[F, Json]

  given circeCodecChallenge: io.circe.Codec[Challenge] = codec[Challenge]
end Challenge
