package com.peknight.acme.error

import com.peknight.acme.identifier.Identifier
import com.peknight.error.Error

case class ACMEError(
                      acmeErrorType: ACMEErrorType,
                      detail: String,
                      identifier: Option[Identifier] = None,
                      subProblems: Option[List[ACMEError]] = None
                    ) extends Error:
  def `type`: String = ACMEError.`type`(acmeErrorType)
  override def message: String = detail
  override def cause: Option[Error] = subProblems.map(Error.apply)
end ACMEError
object ACMEError:
  def `type`(acmeErrorType: ACMEErrorType): String = s"urn:ietf:params:acme:error:$acmeErrorType"
end ACMEError
