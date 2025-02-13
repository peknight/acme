package com.peknight.acme.error.server

trait BadPublicKey extends ACMEServerError:
  def typeLabel: String = "badPublicKey"
  def description: String = "The JWS was signed by a public key the server does not support"
end BadPublicKey
