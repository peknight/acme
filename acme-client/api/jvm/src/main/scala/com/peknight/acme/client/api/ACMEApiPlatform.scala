package com.peknight.acme.client.api

import cats.data.NonEmptyList
import com.peknight.error.Error
import com.peknight.jose.jws.JsonWebSignature
import org.http4s.Uri

import java.security.cert.X509Certificate

trait ACMEApiPlatform[F[_]]:
  def certificate(jws: JsonWebSignature, uri: Uri): F[Either[Error, (NonEmptyList[X509Certificate], Option[List[Uri]])]]
end ACMEApiPlatform
