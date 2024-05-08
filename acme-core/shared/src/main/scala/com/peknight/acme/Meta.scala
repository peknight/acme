package com.peknight.acme

import com.comcast.ip4s.Host
import org.http4s.Uri

case class Meta(caaIdentities: List[Host], termsOfService: Uri, website: Uri)
