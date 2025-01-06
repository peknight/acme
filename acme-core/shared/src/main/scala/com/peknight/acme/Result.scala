package com.peknight.acme

import org.http4s.Headers

case class Result[A](headers: Headers = Headers.empty, body: Option[A] = None)
object Result:
  def empty[A]: Result[A] = Result[A]()
end Result
