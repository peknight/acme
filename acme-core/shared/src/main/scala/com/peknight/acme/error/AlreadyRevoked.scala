package com.peknight.acme.error

trait AlreadyRevoked extends ACMEError:
  def label: String = "alreadyRevoked"
  def description: String = "The request specified a certificate to be revoked that has already been revoked"
end AlreadyRevoked
