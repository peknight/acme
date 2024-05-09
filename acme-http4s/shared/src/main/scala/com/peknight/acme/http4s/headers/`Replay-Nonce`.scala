package com.peknight.acme.http4s.headers

import cats.parse.Parser0
import org.http4s.{ParseResult, ParseResultOps}

final case class `Replay-Nonce`(nonce: String)
object `Replay-Nonce`:
  def parse(s: String): ParseResult[`Replay-Nonce`] =
    ParseResultOps.fromParser(parser, "Invalid Replay-Nonce header")(s)

  private[this] val parser: Parser0[`Replay-Nonce`] = ???
end `Replay-Nonce`
