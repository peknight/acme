package com.peknight.acme.challenge

import cats.effect.Sync
import com.peknight.error.Error

import java.security.PublicKey

trait DNS01Platform:
  def content[F[_]: Sync](publicKey: PublicKey): F[Either[Error, String]]
end DNS01Platform
