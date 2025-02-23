package com.peknight.acme.client.error

import com.peknight.error.Error

case class ProfileNotSupported(profile: String) extends Error:
  override def lowPriorityMessage: Option[String] = Some(s"pofile: $profile not supported")
end ProfileNotSupported
