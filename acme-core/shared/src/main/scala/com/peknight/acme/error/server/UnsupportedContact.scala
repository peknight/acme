package com.peknight.acme.error.server

trait UnsupportedContact extends ACMEServerError:
  def typeLabel: String = "unsupportedContact"
  def description: String = "A contact URL for an account used an unsupported protocol scheme"
end UnsupportedContact
