package com.peknight.acme.server.error

trait OrderNotReady extends ACMEServerError:
  def label: String = "orderNotReady"
  def description: String = "The request attempted to finalize an order that is not ready to be finalized"
end OrderNotReady
