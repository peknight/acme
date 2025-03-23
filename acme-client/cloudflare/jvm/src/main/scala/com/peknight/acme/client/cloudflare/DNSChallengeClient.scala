package com.peknight.acme.client.cloudflare

import cats.Show
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
import com.peknight.cloudflare.dns.record.api.DNSRecordApi
import com.peknight.cloudflare.dns.record.body.DNSRecordBody
import com.peknight.cloudflare.dns.record.body.DNSRecordBody.TXT
import com.peknight.cloudflare.dns.record.error.DNSRecordIdNotMatch
import com.peknight.cloudflare.dns.record.query.ListDNSRecordsQuery
import com.peknight.cloudflare.dns.record.{DNSRecord, DNSRecordId}
import com.peknight.cloudflare.zone.ZoneId
import com.peknight.error.Error
import com.peknight.logging.syntax.eitherF.log
import com.peknight.logging.syntax.eitherT.log
import com.peknight.validation.std.either.isTrue
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.security.PublicKey
import scala.concurrent.duration.*

class DNSChallengeClient[F[_]: {Sync, Logger}](dnsRecordApi: DNSRecordApi[F], zoneId: ZoneId)
  extends api.DNSChallengeClient[F, DNSRecordId]:

  given [A]: Show[A] = Show.fromToString[A]
  def createDNSRecord(identifier: DNS, challenge: `dns-01`, publicKey: PublicKey)
  : F[Either[Error, Option[DNSRecordId]]] =
    val eitherT =
      for
        content <- EitherT(challenge.content[F](publicKey))
        name = challenge.name(identifier)
        _ <- deleteDNSRecords(identifier, challenge)("DNSChallengeClient#createDNSRecord")
        record = TXT(content, name, ttl = Some(1.minute))
        dnsRecord <- EitherT(dnsRecordApi.createDNSRecord(zoneId)(record).asError)
          .log(name = "DNSChallengeClient#createDNSRecord", param = Some(record))
      yield
        dnsRecord.id.some
    eitherT.value

  def cleanDNSRecord(identifier: DNS, challenge: `dns-01`, dnsRecordId: Option[DNSRecordId] = None)
  : F[Either[Error, List[DNSRecordId]]] =
    dnsRecordId match
      case Some(id) => dnsRecordApi.deleteDNSRecord(zoneId, id).asError.map(_.map(List(_)))
        .log(name = "DNSChallengeClient#cleanDNSRecord#deleteDNSRecord", param = Some(id))
      case None => deleteDNSRecords(identifier, challenge)("DNSChallengeClient#cleanDNSRecord").map(_.map(_.id)).value

  private def deleteDNSRecords(identifier: DNS, challenge: `dns-01`)(logName: String): EitherT[F, Error, List[DNSRecord]] =
    val name = challenge.name(identifier).replaceAll("\\.$", "")
    val query = ListDNSRecordsQuery(name = name.some)
    for
      dnsRecords <- EitherT(dnsRecordApi.listDNSRecords(zoneId)(query).asError)
        .log(name = s"$logName#listDNSRecords", param = Some(query))
      dnsRecordIds <- dnsRecords.traverse { dnsRecord =>
        val eitherT =
          for
            dnsRecordId <- EitherT(dnsRecordApi.deleteDNSRecord(zoneId, dnsRecord.id).asError)
            _ <- isTrue(dnsRecordId === dnsRecord.id, DNSRecordIdNotMatch(dnsRecordId, dnsRecord.id)).eLiftET[F]
          yield
            dnsRecordId
        eitherT.log(name = s"$logName#deleteDNSRecord", param = Some(dnsRecord.id))
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
