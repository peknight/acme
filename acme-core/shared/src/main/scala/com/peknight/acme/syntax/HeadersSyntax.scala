package com.peknight.acme.syntax

import cats.effect.Sync
import cats.syntax.eq.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import com.peknight.acme.headers.`Replay-Nonce`
import com.peknight.cats.effect.ext.Clock
import com.peknight.codec.base.Base64UrlNoPad
import org.http4s.CacheDirective.`max-age`
import org.http4s.headers.*
import org.http4s.{Headers, Uri}

import java.time.Instant
import scala.concurrent.duration.*

trait HeadersSyntax:
  extension (headers: Headers)
    def getNonce: Option[Base64UrlNoPad] = headers.get[`Replay-Nonce`].map(_.nonce)
    def getLocation(uri: Uri): Option[Uri] = headers.get[Location].map(loc => uri.resolve(loc.uri))
    def getLastModified: Option[Instant] = headers.get[`Last-Modified`].map(_.date.toInstant)
    def getExpiration[F[_]: Sync]: F[Option[Instant]] =
      headers.get[`Cache-Control`]
        .flatMap { _.values.collectFirst {
          case `max-age`(deltaSeconds) if deltaSeconds =!= 0.second =>
            Clock.realTimeInstant[F].map(_.plusSeconds(deltaSeconds.toSeconds))
        }}
        .sequence
        .map(_.orElse(headers.get[Expires].map(_.expirationDate.toInstant)))
    def getLinks: Option[List[String]] = headers.get[Link].map { _.values.map(_.rel).collect { case Some(rel) => rel } }
    def getRetryAfter: Option[Instant] =
      headers.get[`Retry-After`].flatMap { date =>
        date.retry match
          case Left(date) => Some(date.toInstant)
          case Right(seconds) => headers.get[Date].map(_.date.toInstant.plusSeconds(seconds))
      }
  end extension
end HeadersSyntax
object HeadersSyntax extends HeadersSyntax
