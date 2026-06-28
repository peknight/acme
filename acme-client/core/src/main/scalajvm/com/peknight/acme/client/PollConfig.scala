package com.peknight.acme.client

import cats.MonadError
import cats.effect.std.Env
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.reader.Key

import scala.concurrent.duration.*

case class PollConfig(timeout: FiniteDuration = 1.minute, interval: FiniteDuration = 3.seconds)
object PollConfig:
  val default: PollConfig = PollConfig()
  given keyDecodePollConfig[F[_]](using MonadError[F, Throwable], Env[F]): Decoder[F, Key, PollConfig] =
    Decoder.derivedByKey[F, PollConfig]
end PollConfig
