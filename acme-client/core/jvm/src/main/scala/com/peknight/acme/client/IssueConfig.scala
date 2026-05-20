package com.peknight.acme.client

import cats.data.NonEmptyList
import com.peknight.acme.identifier.Identifier
import com.peknight.error.Error
import com.peknight.security.provider.Provider

import java.security.{KeyPair, Provider as JProvider}
import scala.concurrent.duration.*

case class IssueConfig[F[_]](
                              identifiers: NonEmptyList[Identifier],
                              accountKeyPair: F[Either[Error, KeyPair]],
                              domainKeyPair: F[Either[Error, KeyPair]],
                              postChallengeDelay: FiniteDuration = 2.minutes,
                              challengePoll: PollConfig = PollConfig.default,
                              orderPoll: PollConfig = PollConfig.default,
                              csrProvider: Option[Provider | JProvider] = None
                            )
