package com.tapiporla.microservices.retrievers

import akka.actor.{ActorSystem, Props}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateData
import com.tapiporla.microservices.retrievers.indices.common.ibex35.Ibex35Retriever

import scala.concurrent.duration._

object Main {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("retrievers")
    val testRetriever = system.actorOf(Props[Ibex35Retriever])


    import system.dispatcher
    system.scheduler.scheduleOnce(500 milliseconds) {
      testRetriever ! UpdateData
    }

  }

}
