package com.peknight.acme.error

trait BadCSR extends ACMEError:
  def label: String = "badCSR"
  def description: String = "The CSR is unacceptable (e.g., due to a short key)"
end BadCSR
