package com.tapiporla.microservices.retrievers.common

import akka.actor.{Actor, ActorLogging}

trait TapiporlaActor extends Actor with ActorLogging {

  override def unhandled(message: Any): Unit =
    log.error(s"Unknown message received by actor: $message")

}
