package com.peknight.acme.error.server

trait InvalidContact extends ACMEServerError:
  def label: String = "invalidContact"
  def description: String = "A contact URL for an account was invalid"
end InvalidContact
