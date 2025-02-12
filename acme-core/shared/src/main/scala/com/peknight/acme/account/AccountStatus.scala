package com.peknight.acme.account

import cats.Applicative
import com.peknight.codec.Codec
import com.peknight.codec.configuration.CodecConfiguration
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType

/**
 * Account objects are created in the "valid" state, since no further
 * action is required to create an account after a successful newAccount
 * request.  If the account is deactivated by the client or revoked by
 * the server, it moves to the corresponding state.
 *
 *                    valid
 *                      |
 *                      |
 *          +-----------+-----------+
 *  Client  |                Server |
 * deactiv. |                revoke |
 *          V                       V
 *     deactivated               revoked
 */
enum AccountStatus:
  case valid, deactivated, revoked
end AccountStatus
object AccountStatus:
  given stringCodecAccountStatus[F[_]: Applicative]: Codec[F, String, String, AccountStatus] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, AccountStatus](using CodecConfiguration.default)
  given codecAccountStatus[F[_]: Applicative, S: StringType]: Codec[F, S, Cursor[S], AccountStatus] =
    Codec.codecS[F, S, AccountStatus]
end AccountStatus
