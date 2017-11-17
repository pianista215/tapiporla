package com.tapiporla.microservices.retrievers

import akka.actor.{ActorRef, Props}
import com.tapiporla.microservices.retrievers.BossCoordinator.StartAllCoordinators
import com.tapiporla.microservices.retrievers.common.{TapiporlaActor, TapiporlaConfig}
import com.tapiporla.microservices.retrievers.indices.stock.{StockCoordinator, StockHistoricManager}

object BossCoordinator {

  def props():Props = Props(new BossCoordinator())

  case object StartAllCoordinators
}

class BossCoordinator extends TapiporlaActor{


  override def receive = initial

  def initial: Receive = {

    case StartAllCoordinators =>
      log.info("Initializing BossCoordinator")

      val stockCoordinatorsToLaunch = TapiporlaConfig.Stock.stocks
      log.info(s"Found StockCoordinators: $stockCoordinatorsToLaunch")

      //The actors are autoinitialiced on creation
      val actors = stockCoordinatorsToLaunch.map {stockConfig =>
        context.actorOf(StockCoordinator.props(stockConfig), name = s"${stockConfig.name}StockCoordinator")
      }

      context.become(started(actors))


  }

  def started(coordinatorsLaunched: Seq[ActorRef]): Receive = {
    case _ =>
  }


}
