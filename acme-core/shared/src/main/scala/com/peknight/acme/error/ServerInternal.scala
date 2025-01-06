package com.peknight.acme.error

trait ServerInternal extends ACMEError:
  def label: String = "serverInternal"
  def description: String = "The server experienced an internal error"
end ServerInternal
