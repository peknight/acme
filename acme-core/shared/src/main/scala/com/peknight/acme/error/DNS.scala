package com.peknight.acme.error

trait DNS extends ACMEError:
  def label: String = "dns"
  def description: String = "There was a problem with a DNS query during identifier validation"
end DNS
