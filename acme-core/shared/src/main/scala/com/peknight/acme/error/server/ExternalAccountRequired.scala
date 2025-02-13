package com.peknight.acme.error.server

trait ExternalAccountRequired extends ACMEServerError:
  def typeLabel: String = "externalAccountRequired"
  def description: String = """The request must include a value for the "externalAccountBinding" field"""
end ExternalAccountRequired
