package com.peknight.acme.server.error

trait DNS extends ACMEServerError:
  def label: String = "dns"
  def description: String = "There was a problem with a DNS query during identifier validation"
end DNS
