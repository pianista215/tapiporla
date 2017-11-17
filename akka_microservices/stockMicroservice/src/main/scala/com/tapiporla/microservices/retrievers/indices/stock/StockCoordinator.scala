package com.tapiporla.microservices.retrievers.indices.stock

import akka.actor.{Actor, ActorLogging, Props}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateHistoricData
import com.tapiporla.microservices.retrievers.common.TapiporlaConfig.StockConfig
import com.tapiporla.microservices.retrievers.common.{TapiporlaActor, TapiporlaConfig}
import com.tapiporla.microservices.retrievers.indices.stock.StockHistoricManager.UpdateComplete
import com.tapiporla.microservices.retrievers.indices.stock.StockStatManager.StatsUpdatedSuccessfully

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object StockCoordinator {
  def props(stockConfig: StockConfig): Props = Props(new StockCoordinator(stockConfig))
}

/**
  * Chief actor in charge of coordination between Historic Manager and Stat Manager
  */
class StockCoordinator(stockConfig: StockConfig) extends TapiporlaActor {

  val historicManager =
    context.actorOf(StockHistoricManager.props(stockConfig), name = s"${stockConfig.name}HistoricManager")

  val statManager =
    context.actorOf(StockStatManager.props(stockConfig), name = s"${stockConfig.name}StatManager")

  val executionPeriod: FiniteDuration = TapiporlaConfig.Daemon.periodicExecution

  override def receive: Receive = initial

  def initial: Receive = {

    //TODO: We have to launch the HistoricManager from here and not automatic as now, because we have to now if the market is closed, or not

    case UpdateComplete => //Now we have ES index stable, we proceed to create stats not updated
      statManager ! StockStatManager.InitStatManager
      context.become(ready)

  }

  def ready: Receive = {
    case UpdateComplete => //Just update stats
      statManager ! StockStatManager.UpdateStats

    case StatsUpdatedSuccessfully => //Stats updated correctly

      //TODO: We must schedule next iteration at the time we now the market is closed
      log.info(s"Scheduling another execution in $executionPeriod")
      context.system.scheduler.scheduleOnce(executionPeriod, historicManager, UpdateHistoricData)

  }

}
