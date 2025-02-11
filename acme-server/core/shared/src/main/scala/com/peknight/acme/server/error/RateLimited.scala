package com.peknight.acme.server.error

trait RateLimited extends ACMEServerError:
  def label: String = "rateLimited"
  def description: String = "The request exceeds a rate limit"
end RateLimited
