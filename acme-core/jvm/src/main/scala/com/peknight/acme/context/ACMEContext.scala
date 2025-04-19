package com.peknight.acme.context

import cats.Show
import cats.data.NonEmptyList
import com.peknight.acme.account.Account
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.order.Order
import com.peknight.generic.derivation.show
import com.peknight.jose.jwk.JsonWebKey
import com.peknight.security.certificate.showCertificate
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
object ACMEContext:
  given showACMEContext[Challenge <: com.peknight.acme.challenge.Challenge](using Show[Challenge])
  : Show[ACMEContext[Challenge]] =
    given Show[KeyPair] = JsonWebKey.showKeyPair[KeyPair]
    given Show[X509Certificate] = showCertificate
    show.derived[ACMEContext[Challenge]]

end ACMEContext
