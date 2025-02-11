package com.peknight.acme.server.error

trait IncorrectResponse extends ACMEServerError:
  def label: String = "incorrectResponse"
  def description: String = "Response received didnt match the challenges requirements"
end IncorrectResponse
