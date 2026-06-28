package com.peknight.acme.syntax

import com.peknight.acme.headers.`Replay-Nonce`
import com.peknight.codec.base.Base64UrlNoPad
import org.http4s.Headers

trait HeadersSyntax:
  extension (headers: Headers)
    def getNonce: Option[Base64UrlNoPad] = headers.get[`Replay-Nonce`].map(_.nonce)
  end extension
end HeadersSyntax
object HeadersSyntax extends HeadersSyntax
