package com.peknight.acme.error.server

trait ExternalAccountRequired extends ACMEServerError:
  def label: String = "externalAccountRequired"
  def description: String = """The request must include a value for the "externalAccountBinding" field"""
end ExternalAccountRequired
