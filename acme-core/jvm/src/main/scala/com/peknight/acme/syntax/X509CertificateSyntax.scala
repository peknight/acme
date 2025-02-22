package com.peknight.acme.syntax

import cats.data.EitherT
import cats.effect.Sync
import com.peknight.cats.ext.syntax.eitherT.eLiftET
import com.peknight.codec.base.Base64UrlNoPad
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.security.bouncycastle.cert.X509CertificateHolder
import com.peknight.security.syntax.certificate.getEncodedF
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import scodec.bits.ByteVector

import java.security.cert.X509Certificate

trait X509CertificateSyntax:
  extension (certificate: X509Certificate)
    def getRenewalUniqueIdentifier[F[_]: Sync]: F[Either[Error, String]] =
      val eitherT =
        for
          encoded <- EitherT(certificate.getEncodedF[F].asError)
          holder <- EitherT(X509CertificateHolder[F](encoded).asError)
          akiOption =
            for
              holder <- Option(holder)
              extensions <- Option(holder.getExtensions)
              identifier <- Option(AuthorityKeyIdentifier.fromExtensions(extensions))
              identifier <- Option(identifier.getKeyIdentifier)
            yield
              Base64UrlNoPad.fromByteVector(ByteVector(identifier))
          aki <- akiOption.toRight(OptionEmpty.label("authorityKeyIdentifier")).eLiftET
          snOption =
            for
              holder <- Option(holder)
              structure <- Option(holder.toASN1Structure)
              serialNumber <- Option(structure.getSerialNumber)
              encoded <- Option(serialNumber.getEncoded)
            yield
              Base64UrlNoPad.fromByteVector(ByteVector(encoded).drop(2))
          sn <- snOption.toRight(OptionEmpty.label("serialNumber")).eLiftET
        yield
          s"${aki.value}.${sn.value}"
      eitherT.value
  end extension
end X509CertificateSyntax
object X509CertificateSyntax extends X509CertificateSyntax
