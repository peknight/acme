package com.peknight.acme

import com.peknight.codec.base.Base64Url
import org.http4s.Uri

import java.time.Instant

case class Headers(nonce: Option[Base64Url] = None, location: Option[Uri] = None,
                   lastModified: Option[Instant] = None, expiration: Option[Instant] = None,
                   links: Option[List[String]] = None)
object Headers:
  val empty: Headers = Headers()
end Headers
