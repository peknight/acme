package com.peknight.acme.server.error

trait BadCSR extends ACMEServerError:
  def label: String = "badCSR"
  def description: String = "The CSR is unacceptable (e.g., due to a short key)"
end BadCSR
