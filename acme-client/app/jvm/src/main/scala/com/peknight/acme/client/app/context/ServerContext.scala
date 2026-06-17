package com.peknight.acme.client.app.context

import cats.Show
import cats.data.NonEmptyList
import com.peknight.acme.client.cloudflare.CloudflareDNSChallengeClient
import com.peknight.acme.client.http4s.ACMEClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4s.client.Client

import java.security.cert.X509Certificate
import java.security.{KeyPair, KeyStore}

case class ServerContext[F[_]](
                                streamContext: StreamContext[F],
                                certificates: NonEmptyList[X509Certificate],
                                domainKeyPair: KeyPair,
                                keyStore: KeyStore
                              )
object ServerContext:
  given showServerContext[F[_]]: Show[ServerContext[F]] with
    def show(t: ServerContext[F]): String = "ServerContext(...)"
  end showServerContext
end ServerContext
