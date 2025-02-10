package com.peknight.acme.http

import org.http4s.Headers

import java.time.Instant

case class HttpCache[A](headers: Headers, expiration: Option[Instant], value: A)
