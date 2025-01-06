package com.peknight.acme.error

trait IncorrectResponse extends ACMEError:
  def label: String = "incorrectResponse"
  def description: String = "Response received didnt match the challenges requirements"
end IncorrectResponse
