package com.peknight.acme.error.server

// RFC9115
trait UnknownDelegation extends ACMEServerError:
  def typeLabel: String = "unknownDelegation"
  def description: String = "An unknown configuration is listed in the delegation attribute of the order request"
end UnknownDelegation
