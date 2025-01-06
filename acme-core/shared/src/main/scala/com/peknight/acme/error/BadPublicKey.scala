package com.peknight.acme.error

trait BadPublicKey extends ACMEError:
  def label: String = "badPublicKey"
  def description: String = "The JWS was signed by a public key the server does not support"
end BadPublicKey
