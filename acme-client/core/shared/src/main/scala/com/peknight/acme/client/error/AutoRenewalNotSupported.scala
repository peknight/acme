package com.peknight.acme.client.error

import com.peknight.error.Error

object AutoRenewalNotSupported extends Error:
  override def lowPriorityMessage: Option[String] = Some("auto-renewal not supported")
end AutoRenewalNotSupported
