package com.peknight.acme.error.server

trait Unauthorized extends ACMEServerError:
  def label: String = "unauthorized"
  def description: String = "The client lacks sufficient authorization"
end Unauthorized
