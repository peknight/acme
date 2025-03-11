package com.peknight.acme.challenge

import cats.effect.Sync
import com.peknight.error.Error

import java.security.PublicKey

trait HTTP01Platform:
  def content[F[_]: Sync](publicKey: PublicKey): F[Either[Error, String]]
end HTTP01Platform
