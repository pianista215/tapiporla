package com.tapiporla.microservices

package object retrievers {

  object CausedBy {
    def unapply(e: Throwable): Option[Throwable] = Option(e.getCause)
  }

}
