package com.peknight.acme.client.letsencrypt

import cats.syntax.either.*
import cats.syntax.eq.*
import com.peknight.acme.client.letsencrypt.error.UnknownUri
import com.peknight.acme.client.letsencrypt.host.letsEncrypt
import com.peknight.acme.uri.scheme.acme as acmeScheme
import com.peknight.error.Error
import org.http4s.Uri
import org.http4s.Uri.Host
import org.http4s.Uri.Path.Segment
import org.http4s.syntax.literals.*

package object uri:
  val acme: Uri = uri"acme://letsencrypt.org/"
  val acmeStaging: Uri = uri"acme://letsencrypt.org/staging"
  val directory: Uri = uri"https://acme-v02.api.letsencrypt.org/directory"
  val stagingDirectory: Uri = uri"https://acme-staging-v02.api.letsencrypt.org/directory"

  def accepts(server: Uri): Boolean = server.scheme.contains(acmeScheme) && server.host.contains(Host.fromIp4sHost(letsEncrypt))
  def resolve(server: Uri): Either[Error, Uri] =
    if server.path.isEmpty || server.path.segments === Vector(Segment("v02")) then directory.asRight
    else if server.path.segments === Vector(Segment("staging")) then stagingDirectory.asRight
    else UnknownUri(server).asLeft
end uri
