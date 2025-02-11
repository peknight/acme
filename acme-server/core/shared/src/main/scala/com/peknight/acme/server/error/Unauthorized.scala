package com.peknight.acme.server.error

trait Unauthorized extends ACMEServerError:
  def label: String = "unauthorized"
  def description: String = "The client lacks sufficient authorization"
end Unauthorized
