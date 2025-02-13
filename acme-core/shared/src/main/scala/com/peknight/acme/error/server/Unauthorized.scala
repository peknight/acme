package com.peknight.acme.error.server

trait Unauthorized extends ACMEServerError:
  def typeLabel: String = "unauthorized"
  def description: String = "The client lacks sufficient authorization"
end Unauthorized
