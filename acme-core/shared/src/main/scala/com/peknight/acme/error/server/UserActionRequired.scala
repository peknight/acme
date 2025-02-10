package com.peknight.acme.error.server

trait UserActionRequired extends ACMEServerError:
  def label: String = "userActionRequired"
  def description: String = """Visit the "instance" URL and take actions specified there"""
end UserActionRequired
