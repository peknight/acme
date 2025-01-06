package com.peknight.acme.error

trait UnsupportedContact extends ACMEError:
  def label: String = "unsupportedContact"
  def description: String = "A contact URL for an account used an unsupported protocol scheme"
end UnsupportedContact
