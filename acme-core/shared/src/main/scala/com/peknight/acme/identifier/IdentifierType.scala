package com.peknight.acme.identifier

import cats.Applicative
import com.peknight.codec.Codec
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType

enum IdentifierType:
  case
  // RFC8555
  dns,
  // RFC8738
  ip,
  // RFC8823 RFC5321 RFC6531
  email,
  // RFC9448
  TNAuthList,
  RESERVED
end IdentifierType
object IdentifierType:
  given stringCodecIdentifierType[F[_]: Applicative]: Codec[F, String, String, IdentifierType] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, IdentifierType](using CodecConfiguration.default)
  given codecIdentifierType[F[_]: Applicative, S: StringType]: Codec[F, S, Cursor[S], IdentifierType] =
    Codec.codecS[F, S, IdentifierType]
end IdentifierType
