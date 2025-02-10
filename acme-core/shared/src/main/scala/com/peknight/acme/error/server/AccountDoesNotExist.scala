package com.peknight.acme.error.server

trait AccountDoesNotExist extends ACMEServerError:
  def label: String = "accountDoesNotExist"
  def description: String = "The request specified an account that does not exist"
end AccountDoesNotExist
