package com.peknight.acme.error

trait BadNonce extends ACMEError:
  def label: String = "badNonce"
  def description: String = "The client sent an unacceptable anti-replay nonce"
end BadNonce
