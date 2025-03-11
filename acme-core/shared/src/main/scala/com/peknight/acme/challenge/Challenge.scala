package com.peknight.acme.challenge

import com.peknight.acme.identifier.Identifier.DNS
import com.peknight.acme.validation.ValidationMethod
import com.peknight.acme.validation.ValidationMethod.*
import org.http4s.Uri

trait Challenge:
  def `type`: ValidationMethod
end Challenge
object Challenge:
  trait `http-01` extends Challenge with HTTP01Platform:
    def `type`: ValidationMethod = `http-01`
    def name: String
    def url(identifier: DNS): Uri
  end `http-01`
  trait `dns-01` extends Challenge with DNS01Platform:
    def `type`: ValidationMethod = `dns-01`
    def name(identifier: DNS): String
  end `dns-01`
  trait `tls-sni-01` extends Challenge:
    def `type`: ValidationMethod = `tls-sni-01`
  end `tls-sni-01`
  trait `tls-sni-02` extends Challenge:
    def `type`: ValidationMethod = `tls-sni-02`
  end `tls-sni-02`
  trait `tls-alpn-01` extends Challenge:
    def `type`: ValidationMethod = `tls-alpn-01`
  end `tls-alpn-01`
  trait `email-reply-00` extends Challenge:
    def `type`: ValidationMethod = `email-reply-00`
  end `email-reply-00`
  trait `tkauth-01` extends Challenge:
    def `type`: ValidationMethod = `tkauth-01`
  end `tkauth-01`
  trait `onion-csr-01` extends Challenge:
    def `type`: ValidationMethod = `onion-csr-01`
  end `onion-csr-01`
end Challenge
