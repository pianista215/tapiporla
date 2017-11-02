package com.tapiporla.poc.cluster

import akka.actor.{Actor, ActorLogging}
import akka.event.Logging
import com.tapiporla.poc.cluster.ActorExample.Hello

object ActorExample {
  case class Hello(from: String)
}

class ActorExample extends Actor with ActorLogging{

  def receive = {
    case Hello(from) => log.info(s"Hello from $from")
    case _ => log.info("Undefined message")
  }

}
