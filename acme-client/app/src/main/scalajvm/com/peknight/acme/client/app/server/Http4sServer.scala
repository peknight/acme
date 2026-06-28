package com.peknight.acme.client.app.server

import cats.Parallel
import cats.effect.std.Env
import cats.effect.{Async, Resource}
import cats.syntax.option.*
import com.peknight.acme.client.app.context.{AppContext, ServerContext}
import com.peknight.acme.client.app.stream.ACMEStream
import com.peknight.fs2.syntax.stream.resource
import com.peknight.security.key.store.pkcs12
import fs2.io.file.Files
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder
import org.typelevel.log4cats.Logger

object Http4sServer:
  def apply[F[_]: {Async, Parallel, Logger, Files, Env}](appF: ServerContext[F] => F[WebSocketBuilder[F] => HttpApp[F]])
  : Resource[F, AppContext[F]] =
    for
      streamContext <- ACMEStream[F]
      serverRef <- streamContext.certificates.resource { (certificates, keyPair) =>
        for
          keyStore <- Resource.eval(pkcs12[F](streamContext.config.keyStore.alias, keyPair.getPrivate,
            streamContext.config.keyStore.keyPassword, certificates, none))
          tlsContext <- Resource.eval(Network.forAsync[F].tlsContext.fromKeyStore(keyStore,
            streamContext.config.keyStore.keyPassword.toCharArray))
          serverContext = ServerContext[F](streamContext, certificates, keyPair, keyStore)
          f <- Resource.eval(appF(serverContext))
          server <- EmberServerBuilder.default[F].withLogger(Logger[F])
            .withHostOption(streamContext.config.http.server.host)
            .withPort(streamContext.config.http.server.port)
            .withMaxConnections(streamContext.config.http.server.maxConnections)
            .withReceiveBufferSize(streamContext.config.http.server.receiveBufferSize)
            .withMaxHeaderSize(streamContext.config.http.server.maxHeaderSize)
            .withRequestHeaderReceiveTimeout(streamContext.config.http.server.requestHeaderReceiveTimeout)
            .withIdleTimeout(streamContext.config.http.server.idleTimeout)
            .withShutdownTimeout(streamContext.config.http.server.shutdownTimeout)
            .withTLS(tlsContext)
            .withHttpWebSocketApp(f)
            .build
        yield
          ((certificates, keyPair), server)
      }
    yield
      AppContext(streamContext, serverRef)
end Http4sServer
