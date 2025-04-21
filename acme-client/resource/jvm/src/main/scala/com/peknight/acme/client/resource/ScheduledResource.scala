package com.peknight.acme.client.resource

import cats.data.{EitherT, NonEmptyList}
import cats.effect.*
import cats.syntax.functor.*
import cats.syntax.monadError.*
import cats.syntax.option.*
import com.peknight.acme.challenge.Challenge as ACMEChallenge
import com.peknight.acme.client.api.{ACMEClient, ChallengeClient}
import com.peknight.acme.identifier.Identifier
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.method.cascade.{Source, fetch}
import com.peknight.security.provider.Provider
import com.peknight.security.resource.ScheduledResource as SecurityScheduledResource
import fs2.Stream

import java.security.cert.X509Certificate
import java.security.{KeyPair, KeyStore, Provider as JProvider}
import scala.concurrent.duration.*

object ScheduledResource:
  def apply[F[_], Challenge <: ACMEChallenge, I <: Identifier, Child <: ACMEChallenge, Record, A](
    scheduler: Stream[F, ?],
    accountKeyPair: F[Either[Error, KeyPair]],
    domainKeyPair: F[Either[Error, KeyPair]],
    source: Source[F, (NonEmptyList[X509Certificate], KeyPair)],
    identifiers: NonEmptyList[Identifier] = NonEmptyList.one(Identifier.DNS("*.peknight.com")),
    threshold: FiniteDuration = 7.days,
    alias: String = "",
    keyPassword: String = "",
    sleepAfterPrepare: FiniteDuration = 2.minutes,
    queryChallengeTimeout: FiniteDuration = 1.minutes,
    queryChallengeInterval: FiniteDuration = 3.seconds,
    queryOrderTimeout: FiniteDuration = 1.minutes,
    queryOrderInterval: FiniteDuration = 3.seconds,
    provider: Option[Provider | JProvider] = None
  )(
    resourceF: (KeyStore, NonEmptyList[X509Certificate], KeyPair) => F[Resource[F, A]]
  )(
    using
    acmeClient: ACMEClient[F, Challenge], challengeClient: ChallengeClient[F, Challenge, I, Child, Record],
    async: Async[F])
  : Resource[F, Ref[F, ((NonEmptyList[X509Certificate], KeyPair), A)]] =
    val fetchCertificate = acmeClient.fetchCertificate[I, Child, Record](identifiers, accountKeyPair, domainKeyPair,
        sleepAfterPrepare, queryChallengeTimeout, queryChallengeInterval, queryOrderTimeout, queryOrderInterval,
        provider)
      .map(_.map(context => (context.certificates, context.domainKeyPair)))
    SecurityScheduledResource[F, A](scheduler, threshold, alias, keyPassword, provider)(
      fetch[F, (NonEmptyList[X509Certificate], KeyPair)](source, Source.read(fetchCertificate.map(_.map(_.some))))
        .map(_.flatMap(_.toRight(OptionEmpty.label("certificatesAndKeyPair")))).rethrow
    ) {
      val eitherT =
        for
          (certificates, domainKeyPair) <- EitherT(fetchCertificate)
          _ <- EitherT(source.write((certificates, domainKeyPair)))
        yield
          (certificates, domainKeyPair)
      eitherT.value.rethrow
    }(resourceF)
  end apply
end ScheduledResource
