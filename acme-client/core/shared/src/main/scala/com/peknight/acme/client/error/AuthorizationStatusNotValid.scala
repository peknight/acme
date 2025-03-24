package com.peknight.acme.client.error

import com.peknight.acme.authorization.AuthorizationStatus
import com.peknight.error.Error

case class AuthorizationStatusNotValid(status: AuthorizationStatus) extends Error:
  override def lowPriorityMessage: Option[String] = Some(s"authorization status($status) is not valid")
end AuthorizationStatusNotValid
