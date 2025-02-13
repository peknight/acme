package com.peknight.acme.error.server

// RFC8739
trait AutoRenewalExpired extends ACMEServerError:
  def typeLabel: String = "autoRenewalExpired"
  def description: String = "The short-term certificate is no longer available because the auto-renewal Order has expired"
end AutoRenewalExpired
