package com.peknight.acme.client.stream

import cats.Applicative
import cats.data.{EitherT, NonEmptyList}
import cats.effect.*
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.monadError.*
import cats.syntax.option.*
import cats.syntax.order.*
import com.peknight.acme.challenge.Challenge as ACMEChallenge
import com.peknight.acme.client.IssueConfig
import com.peknight.acme.client.api.{ACMEClient, ChallengeClient}
import com.peknight.acme.identifier.Identifier
import com.peknight.cats.instances.instant.given
import com.peknight.cats.syntax.eitherT.rLiftET
import com.peknight.commons.time.syntax.temporal.-
import com.peknight.error.Error
import com.peknight.error.syntax.applicativeError.asET
import com.peknight.fs2.stream.unfoldTemporal
import com.peknight.security.bouncycastle.openssl.{fetchKeyPair, readX509CertificatesAndKeyPair, writeX509CertificatesAndKeyPair}
import com.peknight.security.provider.Provider
import fs2.io.file.{Files, Path}
import fs2.{Pull, Stream}

import java.security.cert.X509Certificate
import java.security.{KeyPair, Provider as JProvider}
import scala.concurrent.duration.*

object ACMECertificatesStream:
  def apply[F[_], Challenge <: ACMEChallenge, I <: Identifier, Child <: ACMEChallenge, Record, A](
    config: IssueConfig,
    renewalWindow: FiniteDuration = 7.days,
    issueRetryInterval: FiniteDuration = 1.hour,
  )(accountKeyPair: F[Either[Error, KeyPair]], domainKeyPair: F[Either[Error, KeyPair]])(
    using
    acmeClient: ACMEClient[F, Challenge], challengeClient: ChallengeClient[F, Challenge, I, Child, Record],
    temporal: Temporal[F]
  ): Stream[F, (NonEmptyList[X509Certificate], KeyPair)] =
    val issue: F[Either[Error, (NonEmptyList[X509Certificate], KeyPair)]] = acmeClient
      .issue[I, Child, Record](config)(accountKeyPair, domainKeyPair)
      .map(_.map(context => (context.certificates, context.domainKeyPair)))
    unfoldTemporal[F, Unit, (NonEmptyList[X509Certificate], KeyPair)](())(_ => acmeClient
      .issue[I, Child, Record](config)(accountKeyPair, domainKeyPair)
      .map(_.map(context => (context.certificates, context.domainKeyPair)))
      .flatMap {
        case Right((certificates, keyPair)) =>
          interval[F](certificates, renewalWindow, issueRetryInterval).map(i => (((certificates, keyPair), ()).some, i))
        case _ => (none[((NonEmptyList[X509Certificate], KeyPair), Unit)], issueRetryInterval.some).pure[F]
      }
    )
  end apply

  def persisted[F[_], Challenge <: ACMEChallenge, I <: Identifier, Child <: ACMEChallenge, Record](
    config: IssueConfig,
    accountKeyPath: Path,
    domainKeyPath: Path,
    certificatePath: Path,
    renewalWindow: FiniteDuration = 7.days,
    issueRetryInterval: FiniteDuration = 1.hour,
    accountKeyProvider: Option[Provider | JProvider] = None,
    domainKeyProvider: Option[Provider | JProvider] = None,
    certificateProvider: Option[Provider | JProvider] = None,
  )(
    accountKeyPairF: F[Either[Error, KeyPair]],
    domainKeyPairF: F[Either[Error, KeyPair]],
  )(
    using
    acmeClient: ACMEClient[F, Challenge], challengeClient: ChallengeClient[F, Challenge, I, Child, Record],
    async: Async[F], files: Files[F]
  ): Stream[F, (NonEmptyList[X509Certificate], KeyPair)] =
    val eitherT: EitherT[F, Error, Stream[F, (NonEmptyList[X509Certificate], KeyPair)]] =
      for
        accountKeyPair <- EitherT(fetchKeyPair[F](accountKeyPath, accountKeyProvider)(accountKeyPairF))
        acmeStream = apply[F, Challenge, I, Child, Record, Unit](config, renewalWindow, issueRetryInterval)(
          accountKeyPair.asRight[Error].pure[F], domainKeyPairF
        ).evalTap { case (certificates, domainKeyPair) =>
          writeX509CertificatesAndKeyPair[F](certificatePath, domainKeyPath)(certificates, domainKeyPair).rethrow
        }
        option <- EitherT(readX509CertificatesAndKeyPair[F](certificatePath, domainKeyPath, certificateProvider,
          domainKeyProvider))
        stream <- option match
          case Some(tuple @ (certificates, _)) =>
            Option(certificates.head.getNotAfter) match
              case Some(notAfter) =>
                val notAfterInstant = notAfter.toInstant
                Clock[F].realTimeInstant.map { now =>
                  if now >= notAfterInstant then acmeStream
                  else
                    val thresholdInstant = notAfterInstant - renewalWindow
                    val duration =
                      if now < thresholdInstant then (thresholdInstant.toEpochMilli - now.toEpochMilli).millis
                      else issueRetryInterval
                    Pull.output1[F, (NonEmptyList[X509Certificate], KeyPair)](tuple)
                      .flatMap(_ => Pull.sleep(duration))
                      .stream ++ acmeStream
                }.asET
              case _ => acmeStream.rLiftET
          case _ => acmeStream.rLiftET
      yield
        stream
    Stream.eval(eitherT.value.rethrow).flatten
  end persisted

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
