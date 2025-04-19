package com.peknight.acme.client.app.config

import cats.MonadError
import cats.effect.std.Env
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.reader.Key

import scala.concurrent.duration.*

case class HTTPConfig(client: HTTPClientConfig, server: HTTPServerConfig)
object HTTPConfig:
  given decodeHTTPConfigKey[F[_]](using MonadError[F, Throwable], Env[F]): Decoder[F, Key, HTTPConfig] =
    Decoder.derivedByKey[F, HTTPConfig]
end HTTPConfig
