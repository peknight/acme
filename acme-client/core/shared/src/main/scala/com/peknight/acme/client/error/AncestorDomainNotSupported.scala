package com.peknight.acme.client.error

import com.peknight.error.Error

object AncestorDomainNotSupported extends Error:
  override def lowPriorityMessage: Option[String] = Some("ancestor-domain not supported")
end AncestorDomainNotSupported
