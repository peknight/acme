package com.peknight.acme.order

import org.http4s.Uri

case class NewOrderHttpResponse(body: Order, location: Uri)
