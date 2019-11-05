package com.tapiporla.microservices.wallet

import akka.actor.ActorSystem
import com.tapiporla.microservices.wallet.BossCoordinator.StartAllCoordinators

object Main {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("retrievers")
    val bossCoordinator = system.actorOf(BossCoordinator.props(), "BossCoordinator")

    bossCoordinator ! StartAllCoordinators
  }

}
