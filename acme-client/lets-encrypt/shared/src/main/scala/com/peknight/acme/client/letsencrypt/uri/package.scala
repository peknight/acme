package com.peknight.acme.client.letsencrypt

import org.http4s.Uri
import org.http4s.syntax.literals.*

package object uri:
  val acme: Uri = uri"acme://letsencrypt.org/"
  val acmeStaging: Uri = uri"acme://letsencrypt.org/staging"
  val directory: Uri = uri"https://acme-v02.api.letsencrypt.org/directory"
  val stagingDirectory: Uri = uri"https://acme-staging-v02.api.letsencrypt.org/directory"
end uri
