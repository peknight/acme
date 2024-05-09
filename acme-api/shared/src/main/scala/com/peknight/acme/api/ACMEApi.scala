package com.peknight.acme.api

import com.peknight.acme.{Directory, Result}
import org.http4s.Uri

import java.time.ZonedDateTime

trait ACMEApi[F[_]]:
  def directory(uri: Uri)(lastModified: Option[ZonedDateTime]): F[Result[Directory]]
end ACMEApi
