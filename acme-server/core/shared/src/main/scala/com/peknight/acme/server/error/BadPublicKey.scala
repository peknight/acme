package com.peknight.acme.server.error

trait BadPublicKey extends ACMEServerError:
  def label: String = "badPublicKey"
  def description: String = "The JWS was signed by a public key the server does not support"
end BadPublicKey
