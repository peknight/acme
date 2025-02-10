package com.peknight.acme.error.client

import org.http4s.Status

import java.time.Instant

case class NewNonceRateLimited(status: Status, retryAfter: Instant) extends ACMEClientError:
  override def lowPriorityMessage: Option[String] =
    Some(s"Server responded with HTTP ${status.code} while trying to retrieve a nonce, retry after $retryAfter")
end NewNonceRateLimited
