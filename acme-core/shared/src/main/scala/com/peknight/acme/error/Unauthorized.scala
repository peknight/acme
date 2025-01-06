package com.peknight.acme.error

trait Unauthorized extends ACMEError:
  def label: String = "unauthorized"
  def description: String = "The client lacks sufficient authorization"
end Unauthorized
