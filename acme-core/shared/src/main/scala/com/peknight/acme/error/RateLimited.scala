package com.peknight.acme.error

trait RateLimited extends ACMEError:
  def label: String = "rateLimited"
  def description: String = "The request exceeds a rate limit"
end RateLimited
