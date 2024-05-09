package org.http4s

import cats.parse.Parser0

object ParseResultOps:
  def fromParser[A](parser: Parser0[A], errorMessage: => String)(s: String): ParseResult[A] = 
    ParseResult.fromParser[A](parser, errorMessage)(s)
end ParseResultOps
