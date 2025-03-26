package com.peknight.acme.client.api

import com.peknight.error.Error
import com.peknight.http.HttpResponse
import com.peknight.jose.jws.JsonWebSignature
import org.http4s.Uri

import java.security.cert.X509Certificate

trait ACMEApiPlatform[F[_]]:
  def certificates(jws: JsonWebSignature, uri: Uri): F[Either[Error, HttpResponse[List[X509Certificate]]]]
end ACMEApiPlatform
