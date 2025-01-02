package com.peknight.acme

import com.peknight.codec.base.Base64UrlNoPad
import org.http4s.Uri

import java.time.Instant

case class ACMEHeader(
                       nonce: Option[Base64UrlNoPad] = None,
                       location: Option[Uri] = None,
                       lastModified: Option[Instant] = None,
                       expiration: Option[Instant] = None,
                       links: Option[List[String]] = None
                     )
object ACMEHeader:
  val empty: ACMEHeader = ACMEHeader()
end ACMEHeader
