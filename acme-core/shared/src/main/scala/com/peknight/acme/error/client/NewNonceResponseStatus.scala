package com.peknight.acme.error.client

import org.http4s.Status

case class NewNonceResponseStatus(status: Status) extends ACMEClientError:
  override def lowPriorityMessage: Option[String] = Some(s"Server responded with HTTP ${status.code} while trying to retrieve a nonce")
end NewNonceResponseStatus
