package com.peknight.acme.client.stream

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.applicative.*
import cats.syntax.either.*
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
import com.peknight.security.bouncycastle.openssl.{fetchKeyPair, readX509CertificatesAndKeyPair, writeX509CertificatesAndKeyPair}
import com.peknight.security.provider.Provider
import fs2.Stream
import fs2.io.file.{Files, Path}

import java.security.{KeyPair, Provider as JProvider}
import java.security.cert.X509Certificate
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
    certProvider: Option[Provider | JProvider] = None,
    keyProvider: Option[Provider | JProvider] = None,
  )(
    accountKeyPairSource: F[Either[Error, KeyPair]],
    domainKeyPairSource: F[Either[Error, KeyPair]],
  )(
    using
    acmeClient: ACMEClient[F, Challenge], challengeClient: ChallengeClient[F, Challenge, I, Child, Record],
    async: Async[F], files: Files[F]
  ): Stream[F, (NonEmptyList[X509Certificate], KeyPair)] =
    Stream.eval(fetchKeyPair[F](accountKeyPath, keyProvider)(accountKeyPairSource).flatMap {
      case Right(accountKeyPair) => accountKeyPair.pure[F]
      case Left(error) => Sync[F].raiseError[KeyPair](error)
    }).flatMap { accountKeyPair =>
      val acmeStream = apply[F, Challenge, I, Child, Record, Unit](config, renewalWindow, issueRetryInterval)(
        accountKeyPair.asRight[Error].pure[F],
        domainKeyPairSource,
      ).evalTap { case (certificates, domainKeyPair) =>
        writeX509CertificatesAndKeyPair[F](certificatePath, domainKeyPath)(certificates, domainKeyPair).flatMap {
          case Right(()) => ().pure[F]
          case Left(error) => Sync[F].raiseError[Unit](error)
        }
      }
      Stream.eval(readLocalCertificatesAndKeyPair[F](certificatePath, domainKeyPath, certProvider, keyProvider)).flatMap {
        case Some((certificates, domainKeyPair)) =>
          Stream.emit((certificates, domainKeyPair)) ++
            Stream.eval(interval[F](certificates, renewalWindow, issueRetryInterval)).flatMap {
              case Some(duration) => Stream.sleep_[F](duration)
              case None => Stream.empty
            } ++ acmeStream
        case None => acmeStream
      }
    }
  end persisted

  private def readLocalCertificatesAndKeyPair[F[_]](
    certificatePath: Path,
    domainKeyPath: Path,
    certProvider: Option[Provider | JProvider],
    keyProvider: Option[Provider | JProvider],
  )(using Sync[F], Clock[F], Files[F]): F[Option[(NonEmptyList[X509Certificate], KeyPair)]] =
    readX509CertificatesAndKeyPair[F](certificatePath, domainKeyPath, certProvider, keyProvider).flatMap {
      case Left(error) => Sync[F].raiseError[Option[(NonEmptyList[X509Certificate], KeyPair)]](error)
      case Right(Some(value @ (certificates, _))) =>
        notExpired[F](certificates).map(valid => Option.when(valid)(value))
      case Right(None) => none[(NonEmptyList[X509Certificate], KeyPair)].pure[F]
    }

  private def notExpired[F[_]: {Applicative, Clock}](certificates: NonEmptyList[X509Certificate]): F[Boolean] =
    Option(certificates.head.getNotAfter) match
      case Some(notAfter) => Clock[F].realTimeInstant.map(_ < notAfter.toInstant)
      case _ => false.pure[F]

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
