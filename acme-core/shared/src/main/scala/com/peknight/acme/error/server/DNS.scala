package com.peknight.acme.error.server

trait DNS extends ACMEServerError:
  def typeLabel: String = "dns"
  def description: String = "There was a problem with a DNS query during identifier validation"
end DNS
