package com.peknight.acme.client.cloudflare

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.api.syntax.result.asError
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.cloudflare.Result
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.api.DNSRecordApi
import com.peknight.cloudflare.dns.record.body.DNSRecordBody.TXT
import com.peknight.cloudflare.dns.record.query.ListDNSRecordsQuery
import com.peknight.cloudflare.zone.ZoneId
import com.peknight.error.Error

import java.security.PublicKey

class DNSChallengeClient[F[_]: Sync](dnsRecordApi: DNSRecordApi[F], zoneId: ZoneId)
  extends com.peknight.acme.client.api.DNSChallengeClient[F, DNSRecordId]:
  def createDNSRecord(identifier: DNS, challenge: `dns-01`, publicKey: PublicKey)
  : F[Either[Error, Option[DNSRecordId]]] =
    val eitherT =
      for
        content <- EitherT(challenge.content[F](publicKey))
        name = challenge.name(identifier)
        _ <- deleteDNSRecords(identifier, challenge)
        dnsRecord <- EitherT(dnsRecordApi.createDNSRecord(zoneId)(TXT(content, name)).asError)
      yield
        dnsRecord.id.some
    eitherT.value

  def cleanDNSRecord(identifier: DNS, challenge: `dns-01`, dnsRecordId: Option[DNSRecordId] = None)
  : F[Either[Error, List[DNSRecordId]]] =
    dnsRecordId match
      case Some(id) => dnsRecordApi.deleteDNSRecord(zoneId, id).asError.map(_.map(List(_)))
      case None => deleteDNSRecords(identifier, challenge).value

  private def deleteDNSRecords(identifier: DNS, challenge: `dns-01`): EitherT[F, Error, List[DNSRecordId]] =
    for
      dnsRecords <- EitherT(dnsRecordApi.listDNSRecords(zoneId)(
        ListDNSRecordsQuery(name = challenge.name(identifier).some)
      ).asError)
      dnsRecordIds <- dnsRecords.traverse(dnsRecord =>
        EitherT(dnsRecordApi.deleteDNSRecord(zoneId, dnsRecord.id).asError)
      )
    yield
      dnsRecordIds
end DNSChallengeClient
