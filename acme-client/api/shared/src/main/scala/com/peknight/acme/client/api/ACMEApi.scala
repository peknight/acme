package com.peknight.acme.client.api

import cats.Id
import com.peknight.acme.account.Account
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.directory.Directory
import com.peknight.acme.order.Order
import com.peknight.codec.Decoder
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.error.Error
import com.peknight.http.HttpResponse
import com.peknight.jose.jws.JsonWebSignature
import io.circe.Json
import org.http4s.Uri

trait ACMEApi[F[_]] extends ACMEApiPlatform[F]:
  def directory(uri: Uri): F[Either[Error, Directory]]
  def newNonce(uri: Uri): F[Either[Error, Base64UrlNoPad]]
  def newAccount(jws: JsonWebSignature, uri: Uri): F[Either[Error, (Account, Uri)]]
  def account(jws: JsonWebSignature, uri: Uri): F[Either[Error, Account]]
  def newOrder(jws: JsonWebSignature, uri: Uri): F[Either[Error, (Order, Uri)]]
  def order(jws: JsonWebSignature, uri: Uri): F[Either[Error, HttpResponse[Order]]]
  def finalizeOrder(jws: JsonWebSignature, uri: Uri): F[Either[Error, Order]]
  def authorization[Challenge](jws: JsonWebSignature, uri: Uri)(using Decoder[Id, Cursor[Json], Challenge])
  : F[Either[Error, Authorization[Challenge]]]
  def challenge[Challenge](jws: JsonWebSignature, uri: Uri)(using Decoder[Id, Cursor[Json], Challenge])
  : F[Either[Error, HttpResponse[Challenge]]]
end ACMEApi
