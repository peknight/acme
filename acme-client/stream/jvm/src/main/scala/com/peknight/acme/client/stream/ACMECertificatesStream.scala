package com.peknight.acme.client.stream

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*
import com.peknight.acme.challenge.Challenge as ACMEChallenge
import com.peknight.acme.client.api.{ACMEClient, ChallengeClient}
import com.peknight.acme.identifier.Identifier
import com.peknight.cats.instances.instant.given
import com.peknight.commons.time.syntax.temporal.-
import com.peknight.error.Error
import com.peknight.fs2.stream.unfoldTemporal
import com.peknight.security.provider.Provider
import fs2.Stream

import java.security.cert.X509Certificate
import java.security.{KeyPair, Provider as JProvider}
import scala.concurrent.duration.*

object ACMECertificatesStream:
  def apply[F[_], Challenge <: ACMEChallenge, I <: Identifier, Child <: ACMEChallenge, Record, A](
    accountKeyPair: F[Either[Error, KeyPair]],
    domainKeyPair: F[Either[Error, KeyPair]],
    identifiers: NonEmptyList[Identifier],
    threshold: FiniteDuration = 7.days,
    retryInterval: FiniteDuration = 1.hour,
    sleepAfterPrepare: FiniteDuration = 2.minutes,
    queryChallengeTimeout: FiniteDuration = 1.minutes,
    queryChallengeInterval: FiniteDuration = 3.seconds,
    queryOrderTimeout: FiniteDuration = 1.minutes,
    queryOrderInterval: FiniteDuration = 3.seconds,
    csrProvider: Option[Provider | JProvider] = None
  )(
    using
    acmeClient: ACMEClient[F, Challenge], challengeClient: ChallengeClient[F, Challenge, I, Child, Record],
    temporal: Temporal[F]
  ): Stream[F, (NonEmptyList[X509Certificate], KeyPair)] =
    val fetchCertificate: F[Either[Error, (NonEmptyList[X509Certificate], KeyPair)]] = acmeClient
      .fetchCertificate[I, Child, Record](identifiers, accountKeyPair, domainKeyPair, sleepAfterPrepare,
        queryChallengeTimeout, queryChallengeInterval, queryOrderTimeout, queryOrderInterval, csrProvider)
      .map(_.map(context => (context.certificates, context.domainKeyPair)))
    unfoldTemporal[F, Unit, (NonEmptyList[X509Certificate], KeyPair)](()) { _ =>
      fetchCertificate.flatMap {
        case Right((certificates, keyPair)) => Option(certificates.head.getNotAfter).map(_.toInstant - threshold) match
          case Some(thresholdInstant) => Clock[F].realTimeInstant.map { now =>
            val interval =
              if now < thresholdInstant then (thresholdInstant.toEpochMilli - now.toEpochMilli).millis
              else retryInterval
            (((certificates, keyPair), ()).some, interval.some)
          }
          case _ => (((certificates, keyPair), ()).some, none[FiniteDuration]).pure[F]
        case _ => (none[((NonEmptyList[X509Certificate], KeyPair), Unit)], retryInterval.some).pure[F]
      }
    }
  end apply
end ACMECertificatesStream
