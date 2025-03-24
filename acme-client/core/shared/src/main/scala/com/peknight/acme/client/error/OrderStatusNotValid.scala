package com.peknight.acme.client.error

import com.peknight.acme.order.OrderStatus
import com.peknight.error.Error

case class OrderStatusNotValid(status: OrderStatus) extends Error:
  override def lowPriorityMessage: Option[String] = Some(s"order status($status) is not valid")
end OrderStatusNotValid
