package com.peknight.acme.client.api

import com.peknight.jose.jws.JsonWebSignature

case class AccountClaims(
                          contact: Option[List[String]] = None,
                          termsOfServiceAgreed: Option[Boolean] = None,
                          onlyReturnExisting: Option[Boolean] = None,
                          externalAccountBinding: Option[JsonWebSignature] = None
                        )
