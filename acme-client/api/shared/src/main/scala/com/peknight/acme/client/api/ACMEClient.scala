package com.peknight.acme.client.api

import com.peknight.acme.account.{AccountClaims, NewAccountHttpResponse}
import com.peknight.acme.directory.Directory
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error

import java.security.KeyPair

trait ACMEClient[F[_]]:
  def directory: F[Either[Error, Directory]]
  def nonce: F[Either[Error, Base64UrlNoPad]]
  def newAccount(claims: AccountClaims, keyPair: KeyPair): F[Either[Error, NewAccountHttpResponse]]
end ACMEClient
