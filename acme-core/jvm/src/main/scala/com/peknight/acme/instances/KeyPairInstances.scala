package com.peknight.acme.instances

import cats.Show
import com.peknight.codec.syntax.encoder.asS
import cats.Id
import io.circe.Json
import com.peknight.jose.jwk.JsonWebKey

import java.security.KeyPair

trait KeyPairInstances:
  given showKeyPair: Show[KeyPair] with
    def show(t: KeyPair): String =
      JsonWebKey.fromKeyPair(t).map(_.asS[Id, Json].deepDropNullValues.noSpaces).getOrElse(t.toString)
  end showKeyPair
end KeyPairInstances
object KeyPairInstances extends KeyPairInstances
