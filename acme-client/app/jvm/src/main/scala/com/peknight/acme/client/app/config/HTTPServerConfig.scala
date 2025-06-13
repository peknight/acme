package com.peknight.acme.client.app.config

import cats.MonadError
import cats.effect.std.Env
import com.comcast.ip4s.{Host, Port, port}
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.ip4s.instances.port.given
import com.peknight.codec.reader.Key

import scala.concurrent.duration.*

case class HTTPServerConfig(
                             host: Option[Host] = None,
                             port: Port = port"443",
                             maxConnections: Int = 1024,
                             receiveBufferSize: Int = 256 * 1024,
                             maxHeaderSize: Int = 40 * 1024,
                             requestHeaderReceiveTimeout: Duration = 5.seconds,
                             idleTimeout: Duration = 60.seconds,
                             shutdownTimeout: Duration = 30.seconds
                           )
object HTTPServerConfig:
  given keyDecodeHTTPServerConfig[F[_]](using MonadError[F, Throwable], Env[F]): Decoder[F, Key, HTTPServerConfig] =
    Decoder.derivedByKey[F, HTTPServerConfig]
end HTTPServerConfig
