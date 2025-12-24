package com.peknight.acme.client.app.config

import cats.MonadError
import cats.effect.std.Env
import com.peknight.cloudflare.zone.config.CloudflareZoneConfig
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.reader.Key

case class AppConfig(
                      acme: ACMEConfig,
                      http: HTTPConfig,
                      keyStore: KeyStoreConfig,
                      cloudflare: CloudflareZoneConfig
                    )
object AppConfig:
  given keyDecodeAppConfig[F[_]](using MonadError[F, Throwable], Env[F]): Decoder[F, Key, AppConfig] =
    Decoder.derivedByKey[F, AppConfig]
end AppConfig
