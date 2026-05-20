package com.peknight.acme.client.stream

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*
import com.peknight.acme.challenge.Challenge as ACMEChallenge
import com.peknight.acme.client.IssueConfig
import com.peknight.acme.client.api.{ACMEClient, ChallengeClient}
import com.peknight.acme.identifier.Identifier
import com.peknight.cats.instances.instant.given
import com.peknight.commons.time.syntax.temporal.-
import com.peknight.error.Error
import com.peknight.fs2.stream.unfoldTemporal
import fs2.Stream

import java.security.KeyPair
import java.security.cert.X509Certificate
import scala.concurrent.duration.*

object ACMECertificatesStream:
  def apply[F[_], Challenge <: ACMEChallenge, I <: Identifier, Child <: ACMEChallenge, Record, A](
    config: IssueConfig[F],
    threshold: FiniteDuration = 7.days,
    retryInterval: FiniteDuration = 1.hour,
  )(
    using
    acmeClient: ACMEClient[F, Challenge], challengeClient: ChallengeClient[F, Challenge, I, Child, Record],
    temporal: Temporal[F]
  ): Stream[F, (NonEmptyList[X509Certificate], KeyPair)] =
    val issue: F[Either[Error, (NonEmptyList[X509Certificate], KeyPair)]] = acmeClient
      .issue[I, Child, Record](config)
      .map(_.map(context => (context.certificates, context.domainKeyPair)))
    unfoldTemporal[F, Unit, (NonEmptyList[X509Certificate], KeyPair)](())(_ => acmeClient
      .issue[I, Child, Record](config)
      .map(_.map(context => (context.certificates, context.domainKeyPair)))
      .flatMap {
        case Right((certificates, keyPair)) =>
          interval[F](certificates, threshold, retryInterval).map(i => (((certificates, keyPair), ()).some, i))
        case _ => (none[((NonEmptyList[X509Certificate], KeyPair), Unit)], retryInterval.some).pure[F]
      }
    )
  end apply

  private def interval[F[_]: {Applicative, Clock}](certificates: NonEmptyList[X509Certificate],
                                                   threshold: FiniteDuration, retryInterval: FiniteDuration)
  : F[Option[FiniteDuration]] =
    Option(certificates.head.getNotAfter) match
      case Some(notAfter) =>
        val thresholdInstant = notAfter.toInstant - threshold
        Clock[F].realTimeInstant.map { now =>
          if now < thresholdInstant then (thresholdInstant.toEpochMilli - now.toEpochMilli).millis.some
          else retryInterval.some
        }
      case _ => none[FiniteDuration].pure[F]

end ACMECertificatesStream
