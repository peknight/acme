package com.peknight.acme.error.server

trait TLS extends ACMEServerError:
  def label: String = "tls"
  def description: String = "The server received a TLS error during validation"
end TLS
