package com.peknight.acme.server.error

trait BadNonce extends ACMEServerError:
  def label: String = "badNonce"
  def description: String = "The client sent an unacceptable anti-replay nonce"
end BadNonce
