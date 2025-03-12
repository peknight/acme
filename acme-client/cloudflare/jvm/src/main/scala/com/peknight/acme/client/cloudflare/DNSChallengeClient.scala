package com.peknight.acme.client.cloudflare

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.eq.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.client.api
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.api.syntax.result.asError
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.cloudflare.Result
import com.peknight.cloudflare.dns.record.api.DNSRecordApi
import com.peknight.cloudflare.dns.record.body.DNSRecordBody.TXT
import com.peknight.cloudflare.dns.record.error.DNSRecordIdNotMatch
import com.peknight.cloudflare.dns.record.query.ListDNSRecordsQuery
import com.peknight.cloudflare.dns.record.{DNSRecord, DNSRecordId}
import com.peknight.cloudflare.zone.ZoneId
import com.peknight.error.Error
import com.peknight.validation.std.either.isTrue
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.syntax.*

import java.security.PublicKey
import scala.concurrent.duration.*

class DNSChallengeClient[F[_]: {Sync, Logger}](dnsRecordApi: DNSRecordApi[F], zoneId: ZoneId)
  extends api.DNSChallengeClient[F, DNSRecordId]:
  def createDNSRecord(identifier: DNS, challenge: `dns-01`, publicKey: PublicKey)
  : F[Either[Error, Option[DNSRecordId]]] =
    val eitherT =
      for
        content <- EitherT(challenge.content[F](publicKey))
        name = challenge.name(identifier)
        _ <- deleteDNSRecords(identifier, challenge)
        dnsRecord <- EitherT(dnsRecordApi.createDNSRecord(zoneId)(TXT(content, name, ttl = Some(1.minute))).asError)
      yield
        dnsRecord.id.some
    eitherT.value

  def cleanDNSRecord(identifier: DNS, challenge: `dns-01`, dnsRecordId: Option[DNSRecordId] = None)
  : F[Either[Error, List[DNSRecordId]]] =
    dnsRecordId match
      case Some(id) => dnsRecordApi.deleteDNSRecord(zoneId, id).asError.map(_.map(List(_)))
      case None => deleteDNSRecords(identifier, challenge).map(_.map(_.id)).value

  private def deleteDNSRecords(identifier: DNS, challenge: `dns-01`): EitherT[F, Error, List[DNSRecord]] =
    val name = challenge.name(identifier).replaceAll("\\.$", "")
    for
      dnsRecords <- EitherT(dnsRecordApi.listDNSRecords(zoneId)(ListDNSRecordsQuery(name = name.some)).asError)
      dnsRecordIds <- dnsRecords.traverse { dnsRecord =>
        for
          dnsRecordId <- EitherT(dnsRecordApi.deleteDNSRecord(zoneId, dnsRecord.id).asError)
          _ <- isTrue(dnsRecordId === dnsRecord.id, DNSRecordIdNotMatch(dnsRecordId, dnsRecord.id)).eLiftET[F]
        yield
          dnsRecordId
      }
    yield
      dnsRecords
end DNSChallengeClient
object DNSChallengeClient:
  def apply[F[_]](zoneId: ZoneId)(using dnsRecordApi: DNSRecordApi[F], sync: Sync[F])
  : F[api.DNSChallengeClient[F, DNSRecordId]] =
    for
      logger <- Slf4jLogger.fromClass[F](DNSChallengeClient.getClass)
    yield
      given Logger[F] = logger
      new DNSChallengeClient[F](dnsRecordApi, zoneId)
  end apply
end DNSChallengeClient
