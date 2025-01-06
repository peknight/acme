package com.peknight.acme.error

trait UnsupportedIdentifier extends ACMEError:
  def label: String = "unsupportedIdentifier"
  def description: String = "An identifier is of an unsupported type"
end UnsupportedIdentifier
