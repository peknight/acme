package com.peknight.acme.error.server

trait UnsupportedIdentifier extends ACMEServerError:
  def label: String = "unsupportedIdentifier"
  def description: String = "An identifier is of an unsupported type"
end UnsupportedIdentifier
