package com.peknight.acme.client.api

import cats.data.NonEmptyList
import com.peknight.acme.account.{Account, AccountClaims}
import com.peknight.acme.authorization.{Authorization, PreAuthorizationClaims}
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.context.ACMEContext
import com.peknight.acme.directory.Directory
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.order.{FinalizeClaims, Order, OrderClaims}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import com.peknight.http.HttpResponse
import com.peknight.security.certificate.revocation.list.ReasonCode
import org.http4s.Uri

import java.security.cert.{Certificate, X509Certificate}
import java.security.{KeyPair, PublicKey}
import scala.concurrent.duration.*

trait ACMEClient[F[_], Challenge <: com.peknight.acme.challenge.Challenge]:
  def directory: F[Either[Error, Directory]]
  def nonce: F[Either[Error, Base64UrlNoPad]]
  def newAccount(claims: AccountClaims, keyPair: KeyPair): F[Either[Error, (Account, Uri)]]
  def queryAccount(keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]]
  def updateAccount(claims: AccountClaims, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]]
  def deactivateAccount(keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]]
  def keyChange(newKeyPair: KeyPair, oldKeyPair: KeyPair, accountLocation: Uri): F[Either[Error, Account]]
  def newOrder(claims: OrderClaims, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, (Order, Uri)]]
  def queryOrder(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, HttpResponse[Order]]]
  def finalizeOrder(finalizeUri: Uri, claims: FinalizeClaims, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Order]]
  def queryAuthorization(authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Authorization[Challenge]]]
  def deactivateAuthorization(authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Authorization[Challenge]]]
  def preAuthorization(claims: PreAuthorizationClaims, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, (Authorization[Challenge], Uri)]]
  def queryChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, HttpResponse[Challenge]]]
  def updateChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Challenge]]
  def acceptChallenge[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, A](authorization: Authorization[Challenge], publicKey: PublicKey)
                                                                                     (ci: Authorization[Challenge] => Either[Error, (I, C)])
                                                                                     (f: (I, C, PublicKey) => F[Either[Error, Option[A]]])
  : F[Either[Error, Option[(I, C, Option[A])]]]
  def getDnsIdentifierAndChallenge(authorization: Authorization[Challenge]): Either[Error, (DNS, `dns-01`)]
  def downloadCertificate(certificateUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, (NonEmptyList[X509Certificate], Option[List[Uri]])]]
  def revokeCertificate(certificate: Certificate, keyPair: KeyPair, accountLocation: Uri,
                        reason: Option[ReasonCode] = None): F[Either[Error, Unit]]
  def fetchCertificate[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, A](
    identifiers: NonEmptyList[Identifier],
    accountKeyPair: F[Either[Error, KeyPair]],
    domainKeyPair: F[Either[Error, KeyPair]],
    sleepAfterPrepare: Duration = 2.minutes,
    queryChallengeTimeout: FiniteDuration = 1.minutes,
    queryChallengeInterval: FiniteDuration = 3.seconds,
    queryOrderTimeout: FiniteDuration = 1.minutes,
    queryOrderInterval: FiniteDuration = 3.seconds
  )(ic: Authorization[Challenge] => Either[Error, (I, C)]
  )(prepare: (I, C, PublicKey) => F[Either[Error, Option[A]]]
  )(clean: (I, C, Option[A]) => F[Either[Error, Unit]]): F[Either[Error, ACMEContext[Challenge]]]
end ACMEClient
