package com.peknight.acme.client.app.config

import cats.MonadError
import cats.data.NonEmptyList
import cats.effect.std.Env
import com.peknight.acme.client.letsencrypt.uri.acmeStaging
import com.peknight.acme.identifier.Identifier.{DNS, stringDecodeDNS}
import com.peknight.codec.Decoder
import com.peknight.codec.config.given
import com.peknight.codec.effect.instances.envReader.given
import com.peknight.codec.error.DecodingFailure
import com.peknight.codec.fs2.io.instances.path.given
import com.peknight.codec.http4s.instances.uri.given
import com.peknight.codec.reader.Key
import com.peknight.validation.collection.list.either.nonEmpty
import fs2.io.file.Path
import org.http4s.Uri

import scala.concurrent.duration.*

case class ACMEConfig(
                       serverUri: Uri = acmeStaging,
                       accountKeyPath: Path = Path("cert/account.key"),
                       domainKeyPath: Path = Path("cert/domain.key"),
                       certificatePath: Path = Path("cert/domain.crt"),
                       domainIdentifiers: NonEmptyList[DNS] = NonEmptyList.one(DNS("*.peknight.com")),
                       checkInterval: FiniteDuration = 1.day,
                       checkThreshold: FiniteDuration = 7.day,
                       directoryMaxAge: FiniteDuration = 10.minutes,
                       sleepAfterPrepare: FiniteDuration = 2.minutes,
                       queryChallengeTimeout: FiniteDuration = 1.minutes,
                       queryChallengeInterval: FiniteDuration = 3.seconds,
                       queryOrderTimeout: FiniteDuration = 1.minutes,
                       queryOrderInterval: FiniteDuration = 3.seconds,
                       compression: Boolean = true,
                       logHttp: Boolean = true,
                     )
object ACMEConfig:
  given keyDecodeACMEConfig[F[_]](using MonadError[F, Throwable], Env[F]): Decoder[F, Key, ACMEConfig] =
    given stringDecodeNonEmptyListDNS: Decoder[F, String, NonEmptyList[DNS]] =
      Decoder.stringDecodeSeq[F, DNS, NonEmptyList](s =>
        nonEmpty(s.split("\\s*,\\s*").toList).left.map(DecodingFailure.apply)
      )(using stringDecodeDNS)
    Decoder.derivedByKey[F, ACMEConfig]
end ACMEConfig
