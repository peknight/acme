package com.peknight.acme.order

import cats.{Applicative, Eq, Show}
import com.peknight.codec.Codec
import com.peknight.codec.config.CodecConfig
import com.peknight.codec.cursor.Cursor
import com.peknight.codec.derivation.EnumCodecDerivation
import com.peknight.codec.sum.StringType

/**
 * Order objects are created in the "pending" state.  Once all of the
 * authorizations listed in the order object are in the "valid" state,
 * the order transitions to the "ready" state.  The order moves to the
 * "processing" state after the client submits a request to the order's
 * "finalize" URL and the CA begins the issuance process for the
 * certificate.  Once the certificate is issued, the order enters the
 * "valid" state.  If an error occurs at any of these stages, the order
 * moves to the "invalid" state.  The order also moves to the "invalid"
 * state if it expires or one of its authorizations enters a final state
 * other than "valid" ("expired", "revoked", or "deactivated").
 *
 *  pending --------------+
 *     |                  |
 *     | All authz        |
 *     | "valid"          |
 *     V                  |
 *   ready ---------------+
 *     |                  |
 *     | Receive          |
 *     | finalize         |
 *     | request          |
 *     V                  |
 * processing ------------+
 *     |                  |
 *     | Certificate      | Error or
 *     | issued           | Authorization failure
 *     V                  V
 *   valid             invalid
 */
enum OrderStatus:
  case pending, ready, processing, valid, invalid
end OrderStatus
object OrderStatus:
  given eqOrderStatus: Eq[OrderStatus] = Eq.fromUniversalEquals[OrderStatus]
  given stringCodecOrderStatus[F[_]: Applicative]: Codec[F, String, String, OrderStatus] =
    EnumCodecDerivation.unsafeDerivedStringCodecEnum[F, OrderStatus](using CodecConfig.default)
  given codecOrderStatus[F[_]: Applicative, S: {StringType, Show}]: Codec[F, S, Cursor[S], OrderStatus] =
    Codec.codecS[F, S, OrderStatus]
  given showOrderStatus: Show[OrderStatus] = Show.fromToString[OrderStatus]
end OrderStatus
