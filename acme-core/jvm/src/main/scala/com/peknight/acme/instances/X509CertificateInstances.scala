package com.peknight.acme.instances

import cats.Show
import com.peknight.codec.base.Base64UrlNoPad
import scodec.bits.ByteVector

import java.security.cert.X509Certificate

trait X509CertificateInstances:
  given showX509Certificate: Show[X509Certificate] with
    def show(t: X509Certificate): String = Base64UrlNoPad.fromByteVector(ByteVector(t.getEncoded)).value
  end showX509Certificate
end X509CertificateInstances
object X509CertificateInstances extends X509CertificateInstances
