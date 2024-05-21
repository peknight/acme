package com.peknight.acme.http4s.headers

import cats.parse.{Parser, Parser0, Rfc5234}
import com.peknight.codec.base.Base64Url
import org.http4s.{Header, ParseResult, ParseResultOps}
import org.typelevel.ci.CIStringSyntax

final case class `Replay-Nonce`(nonce: Base64Url)
object `Replay-Nonce`:
  def parse(s: String): ParseResult[`Replay-Nonce`] =
    ParseResultOps.fromParser(parser, "Invalid Replay-Nonce header")(s)

  private[this] val parser: Parser0[`Replay-Nonce`] = Base64Url.baseParser.map(`Replay-Nonce`.apply)

  given headerInstance: Header[`Replay-Nonce`, Header.Single] =
    Header.create(ci"Replay-Nonce", v => v.nonce.value, parse)
end `Replay-Nonce`
