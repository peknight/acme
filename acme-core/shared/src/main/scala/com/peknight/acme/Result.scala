package com.peknight.acme

case class Result[A](body: Option[A] = None, headers: Headers = Headers.empty)
object Result:
  def empty[A]: Result[A] = Result[A]()
end Result
