package com.peknight.acme.error.server

trait Compound extends ACMEServerError:
  def typeLabel: String = "compound"
  def description: String = """Specific error conditions are indicated in the "subproblems" array"""
end Compound
