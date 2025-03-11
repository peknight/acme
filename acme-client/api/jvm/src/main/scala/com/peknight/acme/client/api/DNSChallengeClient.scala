package com.peknight.acme.client.api

import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.error.Error

import java.security.PublicKey

trait DNSChallengeClient[F[_], DNSRecordId]:
  def createDNSRecord(challenge: `dns-01`, identifier: DNS, publicKey: PublicKey): F[Either[Error, Option[DNSRecordId]]]
  def deleteDNSRecord(challenge: `dns-01`, identifier: DNS, dnsRecordId: Option[DNSRecordId] = None): F[Either[Error, List[DNSRecordId]]]
end DNSChallengeClient
