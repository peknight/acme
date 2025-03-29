package com.peknight.acme.context

import cats.data.NonEmptyList
import com.peknight.acme.account.Account
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.order.Order
import org.http4s.Uri

import java.security.KeyPair
import java.security.cert.X509Certificate

case class ACMEContext[Challenge <: com.peknight.acme.challenge.Challenge](
                                                                            accountKeyPair: KeyPair,
                                                                            domainKeyPair: KeyPair,
                                                                            certificates: NonEmptyList[X509Certificate],
                                                                            account: Account,
                                                                            accountLocation: Uri,
                                                                            order: Order,
                                                                            orderLocation: Uri,
                                                                            authorizations: List[Authorization[Challenge]],
                                                                            alternates: Option[List[Uri]]
                                                                          )
