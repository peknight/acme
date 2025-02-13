package com.peknight.acme.error.server

trait RateLimited extends ACMEServerError:
  def typeLabel: String = "rateLimited"
  def description: String = "The request exceeds a rate limit"
end RateLimited
