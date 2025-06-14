package com.peknight.acme.client.app.config

import cats.MonadError
import cats.effect.std.Env
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.reader.Key

import scala.concurrent.duration.*

case class HTTPClientConfig(
                             maxTotal: Int = 100,
                             idleTimeInPool: Duration = 30.seconds,
                             chunkSize: Int = 32 * 1024,
                             maxResponseHeaderSize: Int = 4096,
                             idleConnectionTime: Duration = 45.seconds,
                             timeout: Duration = 10.seconds,
                             checkEndpointIdentification: Boolean = true,
                             enableHttp2: Boolean = false,
                             logHeaders: Boolean = true,
                             logBody: Boolean = true
                           )
object HTTPClientConfig:
  given keyDecodeHTTPClientConfig[F[_]](using MonadError[F, Throwable], Env[F]): Decoder[F, Key, HTTPClientConfig] =
    Decoder.derivedByKey[F, HTTPClientConfig]
end HTTPClientConfig
