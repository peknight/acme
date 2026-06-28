package com.peknight.acme.client.error

import com.peknight.acme.order.OrderStatus
import com.peknight.error.Error

case class OrderStatusNotReady(status: OrderStatus) extends Error:
  override def lowPriorityMessage: Option[String] = Some(s"order status($status) is not ready")
end OrderStatusNotReady
