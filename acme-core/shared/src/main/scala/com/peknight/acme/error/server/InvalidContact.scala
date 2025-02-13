package com.peknight.acme.error.server

trait InvalidContact extends ACMEServerError:
  def typeLabel: String = "invalidContact"
  def description: String = "A contact URL for an account was invalid"
end InvalidContact
