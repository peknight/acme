package com.peknight.acme.client.app

import cats.Parallel
import cats.effect.std.Env
import cats.effect.{Async, Resource}
import cats.syntax.applicative.*
import cats.syntax.monadError.*
import cats.syntax.option.*
import com.peknight.acme.challenge.Challenge.`dns-01`
import com.peknight.acme.client.IssueConfig
import com.peknight.acme.client.app.config.AppConfig
import com.peknight.acme.client.app.context.{AppContext, ServerContext}
import com.peknight.acme.client.cloudflare.CloudflareDNSChallengeClient
import com.peknight.acme.client.http4s.ACMEClient
import com.peknight.acme.client.letsencrypt.challenge.Challenge
import com.peknight.acme.client.letsencrypt.uri.resolve
import com.peknight.acme.client.stream.ACMECertificatesStream
import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.cloudflare.dns.record.DNSRecordId
import com.peknight.cloudflare.dns.record.http4s.DNSRecordApi
import com.peknight.codec.Decoder
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.fs2.syntax.stream.resource
import com.peknight.security.Security
import com.peknight.security.bouncycastle.jce.provider.BouncyCastleProvider
import com.peknight.security.cipher.RSA
import com.peknight.security.ecc.sec.secp384r1
import com.peknight.security.key.store.pkcs12
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.client.middleware.Logger as ClientLogger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`User-Agent`
import org.http4s.server.websocket.WebSocketBuilder
import org.typelevel.log4cats.Logger

object ScheduledServer:
  def apply[F[_]: {Async, Parallel, Logger, Files, Env}](appF: ServerContext[F] => F[WebSocketBuilder[F] => HttpApp[F]])
  : Resource[F, AppContext[F]] =
    for
      config <- Resource.eval(Decoder.load[F, AppConfig]().rethrow)
      provider <- Resource.eval(BouncyCastleProvider[F])
      _ <- Resource.eval(Security.addProvider[F](provider))
      client <- EmberClientBuilder.default[F].withLogger(Logger[F])
        .withMaxTotal(config.http.client.maxTotal)
        .withIdleTimeInPool(config.http.client.idleTimeInPool)
        .withChunkSize(config.http.client.chunkSize)
        .withMaxResponseHeaderSize(config.http.client.maxResponseHeaderSize)
        .withIdleConnectionTime(config.http.client.idleConnectionTime)
        .withTimeout(config.http.client.timeout)
        .withUserAgent(config.http.client.userAgent)
        .withCheckEndpointAuthentication(config.http.client.checkEndpointIdentification)
        .withEnableHttp2(config.http.client.enableHttp2)
        .build
      given Client[F] =
        if config.acme.logHttp then ClientLogger(config.http.client.logHeaders, config.http.client.logBody)(client)
        else client
      acmeUri <- Resource.eval(resolve(config.acme.serverUri).pure[F].rethrow)
      acmeClient <- Resource.eval(ACMEClient[F, Challenge](acmeUri, config.acme.directoryMaxAge, None,
        config.acme.compression))
      given ACMEClient[F, Challenge] = acmeClient
      dnsRecordApi = DNSRecordApi[F](config.cloudflare.token)
      given DNSRecordApi[F] = dnsRecordApi
      dnsChallengeClient <- Resource.eval(CloudflareDNSChallengeClient[F, Challenge](config.cloudflare.zoneId))
      given CloudflareDNSChallengeClient[F, Challenge] = dnsChallengeClient
      serverRef <- ACMECertificatesStream.persisted[F, Challenge, DNS, `dns-01`, DNSRecordId](
        IssueConfig(config.acme.domainIdentifiers, config.acme.postChallengeDelay, config.acme.challengePoll, config.acme.orderPoll),
        config.acme.accountKeyPath, config.acme.domainKeyPath, config.acme.certificatePath,
        config.acme.renewalWindow, config.acme.issueRetryInterval, none, provider.some, none,
        provider.some
      )(
        RSA.keySizeGenerateKeyPair[F](4096, provider = provider.some).asError,
        secp384r1.generateKeyPair[F](provider = provider.some).asError
      ).resource{ (certificates, keyPair) =>
        for
          keyStore <- Resource.eval(pkcs12[F](config.keyStore.alias, keyPair.getPrivate, config.keyStore.keyPassword, certificates, none))
          tlsContext <- Resource.eval(Network.forAsync[F].tlsContext.fromKeyStore(keyStore, config.keyStore.keyPassword.toCharArray))
          serverContext = ServerContext[F](client, acmeClient, dnsRecordApi, dnsChallengeClient, certificates,
            keyPair, keyStore, provider)
          f <- Resource.eval(appF(serverContext))
          server <- EmberServerBuilder.default[F].withLogger(Logger[F])
            .withHostOption(config.http.server.host)
            .withPort(config.http.server.port)
            .withMaxConnections(config.http.server.maxConnections)
            .withReceiveBufferSize(config.http.server.receiveBufferSize)
            .withMaxHeaderSize(config.http.server.maxHeaderSize)
            .withRequestHeaderReceiveTimeout(config.http.server.requestHeaderReceiveTimeout)
            .withIdleTimeout(config.http.server.idleTimeout)
            .withShutdownTimeout(config.http.server.shutdownTimeout)
            .withTLS(tlsContext)
            .withHttpWebSocketApp(f)
            .build
        yield
          ((certificates, keyPair), server)
      }
    yield
      AppContext(client, acmeClient, dnsRecordApi, dnsChallengeClient, serverRef, provider)

  extension [F[_]] (builder: EmberClientBuilder[F])
    private def withUserAgent(userAgent: Option[`User-Agent`]): EmberClientBuilder[F] =
      userAgent match
        case Some(ua) => builder.withUserAgent(ua)
        case None => builder.withoutUserAgent
    private def withEnableHttp2(enableHttp2: Boolean): EmberClientBuilder[F] =
      if enableHttp2 then builder.withHttp2 else builder.withoutHttp2
  end extension
end ScheduledServer
