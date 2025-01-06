package com.peknight.acme.error

trait RejectedIdentifier extends ACMEError:
  def label: String = "rejectedIdentifier"
  def description: String = "The server will not issue certificates for the identifier"
end RejectedIdentifier
