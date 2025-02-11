package com.peknight.acme.server.error

trait RejectedIdentifier extends ACMEServerError:
  def label: String = "rejectedIdentifier"
  def description: String = "The server will not issue certificates for the identifier"
end RejectedIdentifier
