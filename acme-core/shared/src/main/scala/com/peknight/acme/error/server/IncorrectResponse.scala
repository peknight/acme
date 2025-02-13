package com.peknight.acme.error.server

trait IncorrectResponse extends ACMEServerError:
  def typeLabel: String = "incorrectResponse"
  def description: String = "Response received didnt match the challenges requirements"
end IncorrectResponse
