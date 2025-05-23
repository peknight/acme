package com.peknight.acme.validation

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType

enum ValidationMethod:
  case `http-01`, `dns-01`, `tls-sni-01`, `tls-sni-02`, `tls-alpn-01`, `email-reply-00`, `tkauth-01`, `onion-csr-01`
end ValidationMethod
object ValidationMethod:
  given stringCodecValidationMethod[F[_]: Applicative]: Codec[F, String, String, ValidationMethod] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, ValidationMethod](using CodecConfig.default)
  given codecValidationMethod[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], ValidationMethod] =
    Codec.codecS[F, S, ValidationMethod]
end ValidationMethod
