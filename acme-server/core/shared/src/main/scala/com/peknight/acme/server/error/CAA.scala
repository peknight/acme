package com.peknight.acme.server.error

trait CAA extends ACMEServerError:
  def label: String = "caa"
  def description: String = "Certification Authority Authorization (CAA) records forbid the CA from issuing a certificate"
end CAA
