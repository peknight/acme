package com.peknight.acme.client.api

import com.peknight.acme.account.NewAccountHttpResponse
import com.peknight.acme.directory.Directory
import com.peknight.acme.order.NewOrderHttpResponse
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import com.peknight.jose.jws.JsonWebSignature
import org.http4s.Uri

trait ACMEApi[F[_]]:
  def directory(uri: Uri): F[Either[Error, Directory]]
  def newNonce(uri: Uri): F[Either[Error, Base64UrlNoPad]]
  def newAccount(jws: JsonWebSignature, uri: Uri): F[Either[Error, NewAccountHttpResponse]]
  def newOrder(jws: JsonWebSignature, uri: Uri): F[Either[Error, NewOrderHttpResponse]]
end ACMEApi
