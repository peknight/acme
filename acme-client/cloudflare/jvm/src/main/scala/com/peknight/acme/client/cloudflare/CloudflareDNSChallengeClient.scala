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
import com.peknight.acme.client.api.DNSChallengeClient
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

class CloudflareDNSChallengeClient[F[_], Challenge <: com.peknight.acme.challenge.Challenge] private (zoneId: ZoneId)(
  using
  dnsRecordApi: DNSRecordApi[F], sync: Sync[F], logger: Logger[F]
) extends DNSChallengeClient[F, Challenge, DNSRecordId]:

  given [A]: Show[A] = Show.fromToString[A]
  def prepare(identifier: DNS, challenge: `dns-01`, publicKey: PublicKey)
  : F[Either[Error, Option[DNSRecordId]]] =
    val eitherT =
      for
        content <- EitherT(challenge.content[F](publicKey))
        name = challenge.name(identifier)
        record = TXT(content, name, ttl = 1.minute.some)
        dnsRecord <- EitherT(dnsRecordApi.createDNSRecord(zoneId)(record).asError)
          .log("DNSChallengeClient#createDNSRecord", record.some)
      yield
        dnsRecord.id.some
    eitherT.value

  def clean(identifier: DNS, challenge: `dns-01`, dnsRecordId: Option[DNSRecordId] = None): F[Either[Error, Unit]] =
    dnsRecordId match
      case Some(id) => dnsRecordApi.deleteDNSRecord(zoneId, id).asError.map(_.as(()))
        .log("DNSChallengeClient#cleanDNSRecord", id.some)
      case None =>
        val name = challenge.name(identifier).replaceAll("\\.$", "")
        val query = ListDNSRecordsQuery(name = name.some)
        val eitherT =
          for
            dnsRecords <- EitherT(dnsRecordApi.listDNSRecords(zoneId)(query).asError)
              .log("DNSChallengeClient#cleanDNSRecord#listDNSRecords", query.some)
            dnsRecordIds <- dnsRecords.traverse { dnsRecord =>
              val eitherT =
                for
                  dnsRecordId <- EitherT(dnsRecordApi.deleteDNSRecord(zoneId, dnsRecord.id).asError)
                  _ <- isTrue(dnsRecordId === dnsRecord.id, DNSRecordIdNotMatch(dnsRecordId, dnsRecord.id)).eLiftET[F]
                yield
                  dnsRecordId
              eitherT.log("DNSChallengeClient#cleanDNSRecord#deleteDNSRecord", dnsRecord.id.some)
            }
          yield
            dnsRecords
        eitherT.as(()).value

end CloudflareDNSChallengeClient
object CloudflareDNSChallengeClient:
  def apply[F[_], Challenge <: com.peknight.acme.challenge.Challenge](zoneId: ZoneId)
                                                                     (using dnsRecordApi: DNSRecordApi[F], sync: Sync[F])
  : F[CloudflareDNSChallengeClient[F, Challenge]] =
    for
      logger <- Slf4jLogger.fromClass[F](CloudflareDNSChallengeClient.getClass)
    yield
      given Logger[F] = logger
      new CloudflareDNSChallengeClient[F, Challenge](zoneId)
  end apply
end CloudflareDNSChallengeClient
