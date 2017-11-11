package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.{Actor, Props}
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35HistoricManager.UpdateComplete

/**
  * Chief actor in charge of coordination between Historic Manager and Stat Manager
  */
class Ibex35Coordinator extends Actor {

  val historicManager =
    context.actorOf(Props[Ibex35HistoricManager], name = "Ibex35HistoricManager")

  val statManager =
    context.actorOf(Props[Ibex35StatManager], name = "Ibex35StatManager")

  override def receive: Receive = initial

  def initial: Receive = {

    case UpdateComplete => //Now we have ES index stable, we proceed to create stats not updated
      statManager ! Ibex35StatManager.InitIbex35StatManager
      context.become(ready)

  }

  def ready: Receive = {
    case UpdateComplete => //Just update stats
      statManager ! Ibex35StatManager.UpdateStats
  }

}
