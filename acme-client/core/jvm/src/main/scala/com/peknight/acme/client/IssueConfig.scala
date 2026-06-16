package com.peknight.acme.client

import cats.data.NonEmptyList
import com.peknight.acme.identifier.Identifier
import com.peknight.security.provider.Provider

import java.security.Provider as JProvider
import scala.concurrent.duration.*

case class IssueConfig(
                        identifiers: NonEmptyList[Identifier],
                        postChallengeDelay: FiniteDuration = 2.minutes,
                        challengePoll: PollConfig = PollConfig.default,
                        orderPoll: PollConfig = PollConfig.default,
                        csrProvider: Option[Provider | JProvider] = None
                      )
