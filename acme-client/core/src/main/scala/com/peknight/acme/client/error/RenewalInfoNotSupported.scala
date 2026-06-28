package com.peknight.acme.client.error

import com.peknight.error.Error

object RenewalInfoNotSupported extends Error:
  override def lowPriorityMessage: Option[String] = Some("renewal-info not supported")
end RenewalInfoNotSupported
