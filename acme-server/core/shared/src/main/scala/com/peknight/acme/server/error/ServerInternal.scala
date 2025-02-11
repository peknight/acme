package com.peknight.acme.server.error

trait ServerInternal extends ACMEServerError:
  def label: String = "serverInternal"
  def description: String = "The server experienced an internal error"
end ServerInternal
