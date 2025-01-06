package com.peknight.acme.error

trait UserActionRequired extends ACMEError:
  def label: String = "userActionRequired"
  def description: String = """Visit the "instance" URL and take actions specified there"""
end UserActionRequired
