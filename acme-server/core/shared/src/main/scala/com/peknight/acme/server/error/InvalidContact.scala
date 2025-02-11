package com.peknight.acme.server.error

trait InvalidContact extends ACMEServerError:
  def label: String = "invalidContact"
  def description: String = "A contact URL for an account was invalid"
end InvalidContact
