package com.peknight.acme.client.api

import com.peknight.acme.directory.Directory
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import org.http4s.Uri

trait ACMEApi[F[_]]:
  def directory(uri: Uri): F[Either[Error, Directory]]
  def newNonce(uri: Uri): F[Either[Error, Base64UrlNoPad]]
end ACMEApi
