package com.peknight.acme.api

import com.peknight.acme.Directory
import com.peknight.error.Error
import org.http4s.Uri

trait ACMEApi[F[_]]:
  def directory(uri: Uri): F[Either[Error, Directory]]
end ACMEApi
