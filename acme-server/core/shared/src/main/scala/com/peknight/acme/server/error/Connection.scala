package com.peknight.acme.server.error

trait Connection extends ACMEServerError:
  def label: String = "connection"
  def description: String = "The server could not connect to validation target"
end Connection
