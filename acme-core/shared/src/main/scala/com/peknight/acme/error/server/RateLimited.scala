package com.peknight.acme.error.server

trait RateLimited extends ACMEServerError:
  def label: String = "rateLimited"
  def description: String = "The request exceeds a rate limit"
end RateLimited
