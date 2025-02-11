package com.peknight.acme.server.error

trait Compound extends ACMEServerError:
  def label: String = "compound"
  def description: String = """Specific error conditions are indicated in the "subproblems" array"""
end Compound
