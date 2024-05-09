package com.peknight.acme

import org.http4s.Uri

case class Directory(keyChange: Uri, meta: Meta, newAccount: Uri, newNonce: Uri, newOrder: Uri, renewalInfo: Uri,
                     revokeCert: Uri, newAuthz: Option[Uri])
