package com.peknight.acme.error

trait CAA extends ACMEError:
  def label: String = "caa"
  def description: String = "Certification Authority Authorization (CAA) records forbid the CA from issuing a certificate"
end CAA
