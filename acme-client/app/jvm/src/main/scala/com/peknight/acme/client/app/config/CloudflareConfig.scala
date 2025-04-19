package com.peknight.acme.client.app.config

import cats.MonadError
import cats.effect.std.Env
import com.peknight.auth.token.Token
import com.peknight.cloudflare.zone.ZoneId
import com.peknight.cloudflare.zone.codec.instances.zoneId.given
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.reader.Key

case class CloudflareConfig(token: Token, zoneId: ZoneId)
object CloudflareConfig:
  given decodeCloudflareConfigKey[F[_]](using MonadError[F, Throwable], Env[F]): Decoder[F, Key, CloudflareConfig] =
    given Decoder[F, String, Token] = Decoder[F, String, String].map(Token.Bearer.apply)
    Decoder.derivedByKey[F, CloudflareConfig]
end CloudflareConfig
