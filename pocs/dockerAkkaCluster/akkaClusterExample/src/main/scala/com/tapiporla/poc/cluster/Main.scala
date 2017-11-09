package com.tapiporla.poc.cluster

import akka.actor.{ActorSystem, Props}
import com.tapiporla.poc.cluster.ActorExample.Hello

import scala.concurrent.duration._

object Main {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("helloworld")
    val hellower = system.actorOf(Props[ActorExample])

    import system.dispatcher
    system.scheduler.scheduleOnce(500 milliseconds) {
      hellower ! Hello("Parent")
    }

  }


}
