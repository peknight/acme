package com.peknight.acme.uri

import org.http4s.Uri.Scheme

package object scheme:
  val acme: Scheme = Scheme.unsafeFromString("acme")
end scheme
