package com.peknight.acme.api

import com.peknight.acme.{Directory, Result}
import org.http4s.Uri

trait ACMEApi[F[_]]:
  def directory(uri: Uri): F[Result[Directory]]
end ACMEApi
