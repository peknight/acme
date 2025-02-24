package com.peknight.acme.client.api

import cats.Id
import com.peknight.acme.account.{AccountClaims, NewAccountHttpResponse, NewAccountResponse}
import com.peknight.acme.authorization.Authorization
import com.peknight.acme.directory.Directory
import com.peknight.acme.order.{NewOrderHttpResponse, Order, OrderClaims}
import com.peknight.codec.Decoder
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.codec.cursor.Cursor
import com.peknight.error.Error
import io.circe.Json
import org.http4s.Uri

import java.security.KeyPair

trait ACMEClient[F[_]]:
  def directory: F[Either[Error, Directory]]
  def nonce: F[Either[Error, Base64UrlNoPad]]
  def newAccount(claims: AccountClaims, keyPair: KeyPair): F[Either[Error, NewAccountHttpResponse]]
  def account(keyPair: KeyPair, accountLocation: Uri): F[Either[Error, NewAccountResponse]]
  def newOrder(claims: OrderClaims, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, NewOrderHttpResponse]]
  def order(orderLocation: Uri, keyPair: KeyPair, accountLocation: Uri): F[Either[Error, Order]]
  def authorization[Challenge](authorizationUri: Uri, keyPair: KeyPair, accountLocation: Uri)
                              (using Decoder[Id, Cursor[Json], Challenge]): F[Either[Error, Authorization[Challenge]]]
end ACMEClient
