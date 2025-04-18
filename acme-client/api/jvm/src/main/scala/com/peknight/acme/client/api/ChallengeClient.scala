package com.peknight.acme.client.api

import com.peknight.acme.authorization.Authorization
import com.peknight.acme.challenge.Challenge as ACMEChallenge
import com.peknight.acme.identifier.Identifier
import com.peknight.error.Error

import java.security.PublicKey

trait ChallengeClient[F[_], Challenge <: ACMEChallenge, I <: Identifier, Child <: ACMEChallenge, Record]:
  def getIdentifierAndChallenge(authorization: Authorization[Challenge]): Either[Error, (I, Child)]
  def prepare(identifier: I, challenge: Child, publicKey: PublicKey): F[Either[Error, Option[Record]]]
  def clean(identifier: I, challenge: Child, record: Option[Record] = None): F[Either[Error, Unit]]
end ChallengeClient
