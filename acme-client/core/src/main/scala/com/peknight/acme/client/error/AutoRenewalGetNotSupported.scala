package com.peknight.acme.client.error

import com.peknight.error.Error

object AutoRenewalGetNotSupported extends Error:
  override def lowPriorityMessage: Option[String] = Some("auto-renewal-get not supported")
end AutoRenewalGetNotSupported
