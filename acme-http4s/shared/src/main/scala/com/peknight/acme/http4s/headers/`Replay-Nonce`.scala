package com.peknight.acme.http4s.headers

import cats.parse.Parser0
import com.peknight.codec.base.Base64UrlNoPad
import org.http4s.{Header, ParseResult, ParseResultOps}
import org.typelevel.ci.CIStringSyntax

final case class `Replay-Nonce`(nonce: Base64UrlNoPad)
object `Replay-Nonce`:
  def parse(s: String): ParseResult[`Replay-Nonce`] =
    ParseResultOps.fromParser(parser, "Invalid Replay-Nonce header")(s)

  private val parser: Parser0[`Replay-Nonce`] = Base64UrlNoPad.baseParser.map(`Replay-Nonce`.apply)

  given headerInstance: Header[`Replay-Nonce`, Header.Single] =
    Header.create(ci"Replay-Nonce", v => v.nonce.value, parse)
end `Replay-Nonce`
