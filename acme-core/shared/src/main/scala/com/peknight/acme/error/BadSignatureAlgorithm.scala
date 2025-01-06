package com.peknight.acme.error

trait BadSignatureAlgorithm extends ACMEError:
  def label: String = "badSignatureAlgorithm"
  def description: String = "The JWS was signed with an algorithm the server does not support"
end BadSignatureAlgorithm
