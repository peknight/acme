package com.peknight.acme

case class Result[A](body: Option[A] = None, headers: ACMEHeader = ACMEHeader.empty)
object Result:
  def empty[A]: Result[A] = Result[A]()
end Result
