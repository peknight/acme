package com.peknight.acme.api

import com.peknight.acme.{Directory, Result}
import org.http4s.Uri

import java.time.Instant

trait ACMEApi[F[_]]:
  def directory(uri: Uri)(lastModified: Option[Instant]): F[Result[Directory]]
end ACMEApi
