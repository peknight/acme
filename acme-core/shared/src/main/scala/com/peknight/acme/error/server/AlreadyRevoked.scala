package com.peknight.acme.error.server

trait AlreadyRevoked extends ACMEServerError:
  def typeLabel: String = "alreadyRevoked"
  def description: String = "The request specified a certificate to be revoked that has already been revoked"
end AlreadyRevoked
