package com.peknight.acme.order

import cats.syntax.either.*
import com.peknight.acme.identifier.Identifier
import com.peknight.acme.identifier.Identifier.{DNS, IP}
import com.peknight.error.Error
import com.peknight.error.std.IllegalArgument
import org.bouncycastle.asn1.x509.{GeneralName, GeneralNames}

trait OrderPlatform { self: Order =>
  def toGeneralNames: Either[Error, GeneralNames] = self.identifiers.traverse {
    case dns: DNS => dns.asciiValue.map(name => GeneralName(GeneralName.dNSName, name))
    case ip: IP => ip.ipAddress.map(address => GeneralName(GeneralName.iPAddress, address.toString))
    case identifier: Identifier => IllegalArgument(identifier).asLeft[GeneralName]
  }.map(generalNames => GeneralNames(generalNames.toList.groupBy(_.getTagNo).toList.sortBy(_._1).flatMap(_._2).toArray))
}
