package com.peknight.acme.error

trait ExternalAccountRequired extends ACMEError:
  def label: String = "externalAccountRequired"
  def description: String = """The request must include a value for the "externalAccountBinding" field"""
end ExternalAccountRequired
