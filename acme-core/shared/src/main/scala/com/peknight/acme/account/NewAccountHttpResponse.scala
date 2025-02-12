package com.peknight.acme.account

import org.http4s.Uri

case class NewAccountHttpResponse(body: NewAccountResponse, location: Uri)
