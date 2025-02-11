package com.peknight.acme.server.error

trait UnsupportedContact extends ACMEServerError:
  def label: String = "unsupportedContact"
  def description: String = "A contact URL for an account used an unsupported protocol scheme"
end UnsupportedContact
