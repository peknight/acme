package com.peknight.acme.order

import com.peknight.acme.identifier.Identifier

import java.time.Instant

case class OrderClaims(
                        identifiers: List[Identifier],
                        notBefore: Option[Instant] = None,
                        notAfter: Option[Instant] = None,
                        autoRenewal: Option[AutoRenewal] = None,
                        replaces: Option[String] = None,
                        profile: Option[String] = None,
                      )
