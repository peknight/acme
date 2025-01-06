package com.peknight.acme.error

trait InvalidContact extends ACMEError:
  def label: String = "invalidContact"
  def description: String = "A contact URL for an account was invalid"
end InvalidContact
