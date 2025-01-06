package com.peknight.acme.error

trait OrderNotReady extends ACMEError:
  def label: String = "orderNotReady"
  def description: String = "The request attempted to finalize an order that is not ready to be finalized"
end OrderNotReady
