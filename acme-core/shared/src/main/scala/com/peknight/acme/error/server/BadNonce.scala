package com.peknight.acme.error.server

trait BadNonce extends ACMEServerError:
  def typeLabel: String = "badNonce"
  def description: String = "The client sent an unacceptable anti-replay nonce"
end BadNonce
