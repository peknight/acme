package com.peknight.acme.client.app.context

import cats.Show
import cats.data.NonEmptyList
import cats.effect.Ref
import com.peknight.acme.client.cloudflare.CloudflareDNSChallengeClient
import com.peknight.acme.client.http4s.ACMEClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4s.client.Client
import org.http4s.server.Server

import java.security.KeyPair
import java.security.cert.X509Certificate

case class AppContext[F[_]](
                             client: Client[F],
                             acmeClient: ACMEClient[F, Challenge],
                             dnsRecordApi: DNSRecordApi[F],
                             dnsChallengeClient: CloudflareDNSChallengeClient[F, Challenge],
                             serverRef: Ref[F, ((NonEmptyList[X509Certificate], KeyPair), Server)],
                             provider: BouncyCastleProvider
                           )
object AppContext:
  given showAppContext[F[_]]: Show[AppContext[F]] with
    def show(t: AppContext[F]): String = "AppContext(...)"
  end showAppContext
end AppContext
