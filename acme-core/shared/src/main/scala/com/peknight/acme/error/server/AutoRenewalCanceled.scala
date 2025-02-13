package com.peknight.acme.error.server

// RFC8739
trait AutoRenewalCanceled extends ACMEServerError:
  def typeLabel: String = "autoRenewalCanceled"
  def description: String = "The short-term certificate is no longer available because the auto-renewal Order has been explicitly canceled by the IdO"
end AutoRenewalCanceled
