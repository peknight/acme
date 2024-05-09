package com.peknight.acme.http4s.headers

import cats.parse.{Parser, Parser0, Rfc5234}
import org.http4s.{Header, ParseResult, ParseResultOps}
import org.typelevel.ci.CIStringSyntax

final case class `Replay-Nonce`(nonce: String)
object `Replay-Nonce`:
  def parse(s: String): ParseResult[`Replay-Nonce`] =
    ParseResultOps.fromParser(parser, "Invalid Replay-Nonce header")(s)

  private[this] val parser: Parser0[`Replay-Nonce`] = 
    ((Rfc5234.alpha | Rfc5234.digit | Parser.charIn("-_")).rep0 ~ Parser.char('=').rep0).string.map(`Replay-Nonce`.apply)

  given headerInstance: Header[`Replay-Nonce`, Header.Single] =
    Header.create(ci"Replay-Nonce", v => v.nonce, parse)
end `Replay-Nonce`
