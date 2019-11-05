package com.tapiporla.microservices

package object wallet {
  object CausedBy {
    def unapply(e: Throwable): Option[Throwable] = Option(e.getCause)
  }
}
