package com.peknight.acme.client.error

import com.peknight.acme.challenge.ChallengeStatus
import com.peknight.error.Error

case class ChallengeStatusNotValid(status: ChallengeStatus) extends Error:
  override def lowPriorityMessage: Option[String] = Some(s"challenge status($status) is not valid")
end ChallengeStatusNotValid
