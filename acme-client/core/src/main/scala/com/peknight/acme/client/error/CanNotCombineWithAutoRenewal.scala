package com.peknight.acme.client.error

import com.peknight.error.Error

case class CanNotCombineWithAutoRenewal(label: String) extends Error:
  override def lowPriorityMessage: Option[String] = Some(s"Cannot combine $label with autoRenewal")
end CanNotCombineWithAutoRenewal
