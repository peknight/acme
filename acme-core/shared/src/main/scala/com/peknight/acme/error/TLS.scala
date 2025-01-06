package com.peknight.acme.error

trait TLS extends ACMEError:
  def label: String = "tls"
  def description: String = "The server received a TLS error during validation"
end TLS
