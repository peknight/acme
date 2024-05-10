package com.peknight.acme

import org.http4s.Uri

import java.time.ZonedDateTime

case class Headers(nonce: Option[String] = None, location: Option[Uri] = None,
                   lastModified: Option[ZonedDateTime] = None, expiration: Option[ZonedDateTime] = None,
                   links: Option[List[String]] = None)
object Headers:
  val empty: Headers = Headers()
end Headers
