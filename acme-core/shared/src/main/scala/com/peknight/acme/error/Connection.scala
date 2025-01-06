package com.peknight.acme.error

trait Connection extends ACMEError:
  def label: String = "connection"
  def description: String = "The server could not connect to validation target"
end Connection
