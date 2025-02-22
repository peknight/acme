package com.peknight.acme.client.api

import com.peknight.error.Error
import org.http4s.Uri

trait ACMEClient[F[_]]:
  def resetNonce(uri: Uri): F[Either[Error, Unit]]
end ACMEClient
