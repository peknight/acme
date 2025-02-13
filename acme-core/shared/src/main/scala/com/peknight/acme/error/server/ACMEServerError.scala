package com.peknight.acme.error.server

import com.peknight.acme.error.ACMEError
import com.peknight.acme.identifier.Identifier
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.error.Error

trait ACMEServerError extends ACMEError:
  def detail: String
  def typeLabel: String
  def description: String
  def `type`: String = s"urn:ietf:params:acme:error:$typeLabel"
  def identifier: Option[Identifier] = None
  def subProblems: Option[List[ACMEServerError]] = None
  override def message: String = detail
  override def cause: Option[Error] = subProblems.map(Error.apply)
end ACMEServerError
object ACMEServerError:
  private val memberNameMap: Map[String, String] = Map(
    "subProblems" -> "subproblems"
  )
  private[server] val codecConfiguration: CodecConfiguration = CodecConfiguration.default
    .withTransformMemberNames(memberName => memberNameMap.getOrElse(memberName, memberName))
    .withExtField("ext")
end ACMEServerError
