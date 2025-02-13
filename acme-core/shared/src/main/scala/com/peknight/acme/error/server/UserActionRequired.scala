package com.peknight.acme.error.server

trait UserActionRequired extends ACMEServerError:
  def typeLabel: String = "userActionRequired"
  def description: String = """Visit the "instance" URL and take actions specified there"""
end UserActionRequired
