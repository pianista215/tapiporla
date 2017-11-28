package com.tapiporla.microservices.wallet

import akka.actor.{ActorRef, Props}
import com.tapiporla.microservices.wallet.BossCoordinator.StartAllCoordinators
import com.tapiporla.microservices.wallet.common.{TapiporlaActor, TapiporlaConfig}

object BossCoordinator {

  def props():Props = Props(new BossCoordinator())

  case object StartAllCoordinators
}

class BossCoordinator extends TapiporlaActor {


  override def receive = initial

  def initial: Receive = {

    case StartAllCoordinators =>
      log.info("Initializing BossCoordinator")

      context.become(started(Seq()))

  }

  def started(coordinatorsLaunched: Seq[ActorRef]): Receive = {
    case message => unhandled(message)
  }


}

