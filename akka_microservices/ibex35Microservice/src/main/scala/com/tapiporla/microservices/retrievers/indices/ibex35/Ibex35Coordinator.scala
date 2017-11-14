package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.{Actor, ActorLogging, Props}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateHistoricData
import com.tapiporla.microservices.retrievers.common.{TapiporlaActor, TapiporlaConfig}
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35HistoricManager.UpdateComplete
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35StatManager.StatsUpdatedSuccessfully

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Chief actor in charge of coordination between Historic Manager and Stat Manager
  */
class Ibex35Coordinator extends TapiporlaActor {

  val historicManager =
    context.actorOf(Props[Ibex35HistoricManager], name = "Ibex35HistoricManager")

  val statManager =
    context.actorOf(Props[Ibex35StatManager], name = "Ibex35StatManager")

  val executionPeriod: FiniteDuration = TapiporlaConfig.Daemon.periodicExecution

  override def receive: Receive = initial

  def initial: Receive = {

    case UpdateComplete => //Now we have ES index stable, we proceed to create stats not updated
      statManager ! Ibex35StatManager.InitIbex35StatManager
      context.become(ready)

  }

  def ready: Receive = {
    case UpdateComplete => //Just update stats
      statManager ! Ibex35StatManager.UpdateStats

    case StatsUpdatedSuccessfully => //Stats updated correctly
      log.info(s"Scheduling another execution in $executionPeriod")
      context.system.scheduler.scheduleOnce(executionPeriod, historicManager, UpdateHistoricData)

  }

}
