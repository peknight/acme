package com.peknight.acme

import java.time.ZonedDateTime

case class Headers(lastModified: Option[ZonedDateTime] = None, expiration: Option[ZonedDateTime] = None,
                   nonce: Option[String] = None)
object Headers:
  val empty: Headers = Headers()
end Headers
