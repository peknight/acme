package com.peknight.acme.error.server

trait Malformed extends ACMEServerError:
  def typeLabel: String = "malformed"
  def description: String = "The request message was malformed"
end Malformed
