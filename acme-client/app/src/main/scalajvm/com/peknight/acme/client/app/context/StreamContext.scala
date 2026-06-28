package com.peknight.acme.client.app.context

import cats.Show
import cats.data.NonEmptyList
import com.peknight.acme.client.app.config.AppConfig
import com.peknight.acme.client.cloudflare.CloudflareDNSChallengeClient
import com.peknight.acme.client.http4s.ACMEClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import fs2.Stream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4s.client.Client

import java.security.KeyPair
import java.security.cert.X509Certificate

case class StreamContext[F[_]](
                                config: AppConfig,
                                client: Client[F],
                                acmeClient: ACMEClient[F, Challenge],
                                dnsRecordApi: DNSRecordApi[F],
                                dnsChallengeClient: CloudflareDNSChallengeClient[F, Challenge],
                                certificates: Stream[F, (NonEmptyList[X509Certificate], KeyPair)],
                                provider: BouncyCastleProvider
                              )
object StreamContext:
  given showStreamContext[F[_]]: Show[StreamContext[F]] with
    def show(t: StreamContext[F]): String = "StreamContext(...)"
  end showStreamContext
end StreamContext
