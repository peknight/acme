package com.peknight.acme.client.api

import cats.Show
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.error.Error
import com.peknight.error.syntax.either.label
import com.peknight.validation.collection.list.either.one
import com.peknight.validation.std.either.typed

trait DNSChallengeClient[F[_], Challenge <: com.peknight.acme.challenge.Challenge, DNSRecordId]
  extends ChallengeClient[F, Challenge, DNS, `dns-01`, DNSRecordId]:
  def getIdentifierAndChallenge(authorization: Authorization[Challenge]): Either[Error, (DNS, `dns-01`)] =
    for
      identifier <- typed[DNS](authorization.identifier).label("identifier")
      dnsChallenges: List[`dns-01`] = authorization.challenges.collect {
        case challenge: `dns-01` => challenge
      }
      challenge <- one(dnsChallenges)(using Show.fromToString).label("dnsChallenges")
    yield
      (identifier, challenge)
end DNSChallengeClient

