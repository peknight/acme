package com.peknight.acme.error.server

// RFC-ietf-acme-onion-07
trait OnionCAARequired extends ACMEServerError:
  def typeLabel: String = "onionCAARequired"
  def description: String = "The CA only supports checking CAA for hidden services in-band, but the client has not provided an in-band CAA"
end OnionCAARequired
