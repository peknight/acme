package com.peknight.acme.error.server

// RFC8739
trait AutoRenewalCancellationInvalid extends ACMEServerError:
  def typeLabel: String = "autoRenewalCancellationInvalid"
  def description: String = "A request to cancel an auto-renewal Order that is not in state \"valid\" has been received"
end AutoRenewalCancellationInvalid
