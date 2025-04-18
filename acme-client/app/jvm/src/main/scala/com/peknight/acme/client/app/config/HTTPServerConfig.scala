package com.peknight.acme.client.app.config

import cats.effect.Async
import cats.effect.std.Env
import com.comcast.ip4s.{Host, Port, port}
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.ip4s.instances.host.given
import com.peknight.codec.ip4s.instances.port.given
import com.peknight.codec.reader.Key

case class HTTPServerConfig(
                             host: Option[Host] = None,
                             port: Port = port"443",
                             logHeaders: Boolean = true,
                             logBody: Boolean = true
                           )
object HTTPServerConfig:
  given decodeHTTPServerConfigKey[F[_]: {Async, Env}]: Decoder[F, Key, HTTPServerConfig] =
    Decoder.derivedByKey[F, HTTPServerConfig]
end HTTPServerConfig
