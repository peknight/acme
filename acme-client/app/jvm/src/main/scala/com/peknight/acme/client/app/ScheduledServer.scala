package com.peknight.acme.client.app

import cats.Parallel
import cats.data.NonEmptyList
import cats.effect.std.Env
import cats.effect.{Async, Resource}
import cats.syntax.applicative.*
import cats.syntax.functor.*
import cats.syntax.monadError.*
import cats.syntax.option.*
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.client.app.config.AppConfig
import com.peknight.acme.client.app.context.{AppContext, ServerContext}
import com.peknight.acme.client.cloudflare.CloudflareDNSChallengeClient
import com.peknight.acme.client.http4s.ACMEClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.acme.client.letsencrypt.uri.resolve
import com.peknight.acme.client.resource.ScheduledResource
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.codec.Decoder
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.method.cascade.Source
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.bouncycastle.openssl.{fetchKeyPair, readX509CertificatesAndKeyPair, writeX509CertificatesAndKeyPair}
import com.peknight.security.cipher.RSA
import com.peknight.security.ecc.sec.secp384r1
import fs2.Stream
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.client.middleware.Logger as ClientLogger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger as ServerLogger
import org.typelevel.log4cats.Logger

import java.security.KeyPair
import java.security.cert.X509Certificate

object ScheduledServer:
  def apply[F[_]: {Async, Parallel, Logger, Files, Env}](httpApp: ServerContext[F] => HttpApp[F])
  : Resource[F, AppContext[F]] =
    for
      config <- Resource.eval(Decoder.load[F, AppConfig]().rethrow)
      provider <- Resource.eval(BouncyCastleProvider[F])
      _ <- Resource.eval(Security.addProvider[F](provider))
      client <- EmberClientBuilder.default[F].withLogger(Logger[F]).withTimeout(config.http.client.timeout).build
      loggerClient = ClientLogger(config.http.client.logHeaders, config.http.client.logBody)(client)
      given Client[F] = loggerClient
      acmeUri <- Resource.eval(resolve(config.acme.serverUri).pure[F].rethrow)
      acmeClient <- Resource.eval(ACMEClient[F, Challenge](acmeUri, config.acme.directoryMaxAge, None,
        config.acme.compression))
      given ACMEClient[F, Challenge] = acmeClient
      dnsRecordApi = DNSRecordApi[F](config.cloudflare.token)
      given DNSRecordApi[F] = dnsRecordApi
      dnsChallengeClient <- Resource.eval(CloudflareDNSChallengeClient[F, Challenge](config.cloudflare.zoneId))
      given CloudflareDNSChallengeClient[F, Challenge] = dnsChallengeClient
      serverRef <- ScheduledResource[F, Challenge, DNS, `dns-01`, DNSRecordId, Server](
        Stream.awakeEvery[F](config.acme.checkInterval),
        fetchKeyPair[F](config.acme.accountKeyPath)(RSA.keySizeGenerateKeyPair[F](4096, provider = provider.some).asError),
        secp384r1.generateKeyPair[F](provider = provider.some).asError,
        Source[F, (NonEmptyList[X509Certificate], KeyPair)](
          readX509CertificatesAndKeyPair(config.acme.certificatePath, config.acme.domainKeyPath, none, provider.some),
          writeX509CertificatesAndKeyPair(config.acme.certificatePath, config.acme.domainKeyPath)
        ),
        config.acme.domainIdentifiers, config.acme.checkThreshold, config.keyStore.alias, config.keyStore.keyPassword,
        config.acme.sleepAfterPrepare, config.acme.queryChallengeTimeout, config.acme.queryChallengeInterval,
        config.acme.queryOrderTimeout, config.acme.queryOrderInterval, provider.some, none
      ) { (keyStore, certificates, keyPair) =>
        Network.forAsync[F].tlsContext.fromKeyStore(keyStore, config.keyStore.keyPassword.toCharArray)
          .map { tlsContext => EmberServerBuilder.default[F].withLogger(Logger[F])
            .withHostOption(config.http.server.host)
            .withPort(config.http.server.port)
            .withTLS(tlsContext)
            .withHttpWebSocketApp { builder =>
              val serverContext = ServerContext[F](builder, client, loggerClient, acmeClient, dnsRecordApi,
                dnsChallengeClient, certificates, keyPair, keyStore, provider)
              val serverConfig = config.http.server
              ServerLogger.httpApp[F](serverConfig.logHeaders, serverConfig.logBody)(httpApp(serverContext))
            }
            .build
          }
      }
    yield
      AppContext(client, loggerClient, acmeClient, dnsRecordApi, dnsChallengeClient, serverRef, provider)
end ScheduledServer
