package com.peknight.acme.account

import cats.{Applicative, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.CodecConfig
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
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, AccountStatus](using CodecConfig.default)
  given codecAccountStatus[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], AccountStatus] =
    Codec.codecS[F, S, AccountStatus]
  given showAccountStatus: Show[AccountStatus] = Show.fromToString[AccountStatus]
end AccountStatus
