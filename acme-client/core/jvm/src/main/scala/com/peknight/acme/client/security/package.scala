package com.peknight.acme.client

import cats.data.EitherT
import cats.effect.{Resource, Sync}
import cats.syntax.functor.*
import cats.syntax.option.*
import com.peknight.cats.ext.syntax.eitherT.{eLiftET, rLiftET}
import com.peknight.error.Error
import com.peknight.error.option.OptionEmpty
import com.peknight.error.syntax.applicativeError.asError
import com.peknight.fs2.io.ext.syntax.path.exists
import com.peknight.method.cascade.{Source, fetch}
import com.peknight.security.bouncycastle.openssl.PEMParser
import com.peknight.security.bouncycastle.openssl.jcajce.JcaPEMWriter
import com.peknight.security.bouncycastle.pkix.syntax.jcaPEMKeyConverter.getKeyPairF
import com.peknight.security.bouncycastle.pkix.syntax.jcaPEMWriter.writeObjectF
import com.peknight.security.bouncycastle.pkix.syntax.pemParser.readObjectF
import fs2.io.file.Path
import org.bouncycastle.openssl.jcajce.{JcaPEMKeyConverter, JcaPEMWriter as JJcaPEMWriter}
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser as JPEMParser}

import java.security.KeyPair

package object security:
  private def readPemKeyPair[F[_]: Sync](path: Path): F[Either[Error, Option[KeyPair]]] =
    val eitherT =
      for
        e <- EitherT(path.exists[F].asError)
        keyPair <-
          if e then
            for
              pemKeyPair <- EitherT(Resource.fromAutoCloseable[F, JPEMParser](PEMParser[F](path))
                .use(_.readObjectF[F, PEMKeyPair]).asError.map(_.flatten))
              keyPair <- EitherT(JcaPEMKeyConverter().getKeyPairF[F](pemKeyPair).asError)
            yield
              keyPair.some
          else
            none[KeyPair].rLiftET[F, Error]
      yield
        keyPair
    eitherT.value

  private def writePemKeyPair[F[_]: Sync](path: Path)(keyPair: KeyPair): F[Either[Error, Unit]] =
    Resource.fromAutoCloseable[F, JJcaPEMWriter](JcaPEMWriter[F](path)).use(_.writeObjectF[F](keyPair).asError)
      .asError.map(_.flatten)

  def fetchKeyPair[F[_]: Sync](path: Path)(source: F[Either[Error, KeyPair]]): F[Either[Error, KeyPair]] =
    val eitherT =
      for
        keyPair <- EitherT(fetch(
          Source(readPemKeyPair[F](path), writePemKeyPair[F](path)),
          Source.read(source.map(_.map(_.some)))
        ))
        keyPair <- keyPair.toRight(OptionEmpty.label("keyPair")).eLiftET[F]
      yield
        keyPair
    eitherT.value
end security
