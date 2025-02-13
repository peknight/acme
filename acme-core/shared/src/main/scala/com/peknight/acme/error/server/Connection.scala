package com.peknight.acme.error.server

trait Connection extends ACMEServerError:
  def typeLabel: String = "connection"
  def description: String = "The server could not connect to validation target"
end Connection
