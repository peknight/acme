package com.peknight.acme.client.app.config

import cats.effect.Async
import cats.effect.std.Env
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.reader.Key

case class KeyStoreConfig(
                           alias: String = "",
                           keyPassword: String = ""
                         )
object KeyStoreConfig:
  given decodeKeyStoreConfigKey[F[_]: {Async, Env}]: Decoder[F, Key, KeyStoreConfig] =
    Decoder.derivedByKey[F, KeyStoreConfig]
end KeyStoreConfig
