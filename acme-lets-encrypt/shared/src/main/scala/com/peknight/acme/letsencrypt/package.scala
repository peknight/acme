package com.peknight.acme

import cats.syntax.either.*
import com.peknight.acme.letsencrypt.error.UnknownUri
import com.peknight.acme.letsencrypt.host.letsEncrypt
import com.peknight.acme.letsencrypt.uri.{directory, stagingDirectory}
import com.peknight.acme.uri.scheme.acme
import com.peknight.error.Error
import org.http4s.Uri
import org.http4s.Uri.Host
import org.http4s.Uri.Path.Segment

package object letsencrypt:
  given CanEqual[Vector[Segment], Vector[Segment]] = CanEqual.derived
  def accepts(server: Uri): Boolean = server.scheme.contains(acme) && server.host.contains(Host.fromIp4sHost(letsEncrypt))
  def resolve(server: Uri): Either[Error, Uri] =
    if server.path.isEmpty || server.path.segments == Vector(Segment("v02")) then directory.asRight
    else if server.path.segments == Vector(Segment("staging")) then stagingDirectory.asRight
    else UnknownUri(server).asLeft
end letsencrypt
