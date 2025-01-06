package com.peknight.acme.error

trait BadRevocationReason extends ACMEError:
  def label: String = "badRevocationReason"
  def description: String = "The revocation reason provided is not allowed by the server"
end BadRevocationReason
