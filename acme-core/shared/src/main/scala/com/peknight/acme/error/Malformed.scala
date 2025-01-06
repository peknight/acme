package com.peknight.acme.error

trait Malformed extends ACMEError:
  def label: String = "malformed"
  def description: String = "The request message was malformed"
end Malformed
