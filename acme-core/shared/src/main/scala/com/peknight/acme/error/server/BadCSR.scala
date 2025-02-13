package com.peknight.acme.error.server

trait BadCSR extends ACMEServerError:
  def typeLabel: String = "badCSR"
  def description: String = "The CSR is unacceptable (e.g., due to a short key)"
end BadCSR
