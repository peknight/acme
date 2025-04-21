package com.peknight.acme.bouncycastle.pkcs

import cats.effect.Sync
import cats.syntax.functor.*
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.security.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import com.peknight.security.provider.Provider
import com.peknight.security.signature.{SHA256withECDSA, SHA256withRSA}
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.{X500Name, X500NameBuilder}
import org.bouncycastle.asn1.x509.{Extension, ExtensionsGenerator, GeneralNames}
import org.bouncycastle.pkcs.PKCS10CertificationRequest as JPKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder

import java.security.interfaces.ECKey
import java.security.{KeyPair, Provider as JProvider}

object PKCS10CertificationRequest:
  def certificateSigningRequest[F[_]: Sync](generalNames: GeneralNames, keyPair: KeyPair,
                                            provider: Option[Provider | JProvider] = None)
  : F[Either[Error, JPKCS10CertificationRequest]] =
    Sync[F].blocking {
      val extensionsGenerator = new ExtensionsGenerator
      extensionsGenerator.addExtension(Extension.subjectAlternativeName, false, generalNames)
      val privateKey = keyPair.getPrivate
      val signatureAlgorithm = if privateKey.isInstanceOf[ECKey] then SHA256withECDSA else SHA256withRSA
      val signer = JcaContentSignerBuilder(signatureAlgorithm, provider).build(privateKey)
      Option(new JcaPKCS10CertificationRequestBuilder(new X500NameBuilder(X500Name.getDefaultStyle).build, keyPair.getPublic)
        .addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensionsGenerator.generate)
        .build(signer))
        .toRight(OptionEmpty.label("PKCS10CertificationRequest"))
    }.asError.map(_.flatten)
end PKCS10CertificationRequest
