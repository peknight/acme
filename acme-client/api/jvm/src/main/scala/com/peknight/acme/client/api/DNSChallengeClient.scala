package com.peknight.acme.client.api

import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.error.Error

import java.security.PublicKey

trait DNSChallengeClient[F[_], DNSRecordId]:
  def createDNSRecord(identifier: DNS, challenge: `dns-01`, publicKey: PublicKey): F[Either[Error, Option[DNSRecordId]]]
  def cleanDNSRecord(identifier: DNS, challenge: `dns-01`, dnsRecordId: Option[DNSRecordId] = None)
  : F[Either[Error, List[DNSRecordId]]]
end DNSChallengeClient
