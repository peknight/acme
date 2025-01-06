package com.peknight.acme.error

trait Compound extends ACMEError:
  def label: String = "compound"
  def description: String = """Specific error conditions are indicated in the "subproblems" array"""
end Compound
