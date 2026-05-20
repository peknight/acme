package com.peknight.acme.client

import scala.concurrent.duration.*

case class PollConfig(timeout: FiniteDuration = 1.minute, interval: FiniteDuration = 3.seconds)
object PollConfig:
  val default: PollConfig = PollConfig()
end PollConfig
