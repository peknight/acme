package com.peknight.acme.error.server

trait ServerInternal extends ACMEServerError:
  def typeLabel: String = "serverInternal"
  def description: String = "The server experienced an internal error"
end ServerInternal
