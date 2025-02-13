package com.peknight.acme.error.server

trait BadRevocationReason extends ACMEServerError:
  def typeLabel: String = "badRevocationReason"
  def description: String = "The revocation reason provided is not allowed by the server"
end BadRevocationReason
