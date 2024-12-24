package com.peknight.acme.letsencrypt.error

import org.http4s.Uri
import com.peknight.error.std.IllegalArgument

case class UnknownUri(argument: Uri) extends IllegalArgument[Uri]:
  override protected def lowPriorityLabelMessage(label: String): Option[String] = Some(s"Unknown Uri $label: $argument")
  override protected def lowPriorityMessage: Option[String] = Some(s"Unknown Uri: $argument")
end UnknownUri
