package com.peknight.acme.error.server

// RFC8739
trait AutoRenewalRevocationNotSupported extends ACMEServerError:
  def typeLabel: String = "autoRenewalRevocationNotSupported"
  def description: String = "A request to revoke an auto-renewal Order has been received"
end AutoRenewalRevocationNotSupported
