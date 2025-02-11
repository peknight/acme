package com.peknight.acme.server.error

trait BadRevocationReason extends ACMEServerError:
  def label: String = "badRevocationReason"
  def description: String = "The revocation reason provided is not allowed by the server"
end BadRevocationReason
