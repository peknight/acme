package com.peknight.acme.error

trait AccountDoesNotExist extends ACMEError:
  def label: String = "accountDoesNotExist"
  def description: String = "The request specified an account that does not exist"
end AccountDoesNotExist
