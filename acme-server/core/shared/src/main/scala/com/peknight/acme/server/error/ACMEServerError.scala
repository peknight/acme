package com.peknight.acme.server.error

import com.peknight.acme.Identifier
import com.peknight.acme.error.ACMEError
import com.peknight.error.Error

trait ACMEServerError extends ACMEError:
  def label: String
  def description: String
  override protected def labelOption: Option[String] = Some(label)
  def `type`: String = s"urn:ietf:params:acme:error:$label"
  def detail: String = message
  def identifier: Option[Identifier] = None
  def subproblems: Option[List[ACMEServerError]] = None
  override def cause: Option[Error] = subproblems.map(Error.apply)
end ACMEServerError
