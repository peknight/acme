package com.peknight.acme.error.server

import com.peknight.acme.identifier.Identifier
import com.peknight.codec.circe.Ext
import io.circe.JsonObject
import org.http4s.Status

case class AccountDoesNotExist(detail: String,
                               status: Option[Status] = None,
                               override val identifier: Option[Identifier] = None,
                               override val subProblems: Option[List[ACMEServerError]] = None,
                               ext: JsonObject = JsonObject.empty
                              ) extends ACMEServerError with Ext:
  def typeLabel: String = "accountDoesNotExist"
  def description: String = "The request specified an account that does not exist"
end AccountDoesNotExist
object AccountDoesNotExist:
  
end AccountDoesNotExist
