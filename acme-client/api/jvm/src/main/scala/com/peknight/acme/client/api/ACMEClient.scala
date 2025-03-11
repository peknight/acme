package com.peknight.acme.client.api

import com.peknight.acme.account.{AccountClaims, NewAccountHttpResponse, NewAccountResponse}
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.directory.Directory
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.order.{NewOrderHttpResponse, Order, OrderClaims}
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import org.http4s.Uri

import java.security.{KeyPair, PublicKey}

trait ACMEClient[F[_], Challenge <: com.peknight.acme.challenge.Challenge]:
  def directory: F[Either[Error, Directory]]
  def nonce: F[Either[Error, Base64UrlNoPad]]
  def newAccount(claims: AccountClaims, keyPair: KeyPair): F[Either[Error, NewAccountHttpResponse]]
  def account(keyPair: KeyPair, accountLocation: Uri): F[Either[Error, NewAccountResponse]]
  def newOrder(claims: OrderClaims, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, NewOrderHttpResponse]]
  def order(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Order]]
  def authorization(authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
  : F[Either[Error, Authorization[Challenge]]]
  def challenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Challenge]]
  def respondToChallenge(challengeUri: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Challenge]]
  def challenge[I <: Identifier, C <: com.peknight.acme.challenge.Challenge, A](authorization: Authorization[Challenge])
                                                                               (ci: => Either[Error, (I, C)])
                                                                               (f: (I, C) => F[Either[Error, Option[A]]])
  : F[Either[Error, Option[(I, C, Option[A])]]]
  def getDnsIdentifierAndChallenge(authorization: Authorization[Challenge]): Either[Error, (DNS, `dns-01`)]
  def createDNSRecord[DNSRecordId](identifier: DNS, challenge: `dns-01`, publicKey: PublicKey)
                                  (using dnsChallengeClient: DNSChallengeClient[F, DNSRecordId])
  : F[Either[Error, Option[DNSRecordId]]]
  def cleanDNSRecords[DNSRecordId](identifier: DNS, challenge: `dns-01`, dnsRecordId: Option[DNSRecordId])
                                  (using dnsChallengeClient: DNSChallengeClient[F, DNSRecordId])
  : F[Either[Error, List[DNSRecordId]]]
end ACMEClient
