package com.tapiporla.microservices.retrievers

import akka.actor.ActorSystem
import com.tapiporla.microservices.retrievers.BossCoordinator.StartAllCoordinators

object Main {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("retrievers")
    val bossCoordinator = system.actorOf(BossCoordinator.props())

    bossCoordinator ! StartAllCoordinators
  }

}
