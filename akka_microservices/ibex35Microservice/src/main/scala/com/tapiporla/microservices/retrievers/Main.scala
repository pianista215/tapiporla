package com.tapiporla.microservices.retrievers

import akka.actor.{ActorSystem, Props}
import com.tapiporla.microservices.retrievers.indices.stock.StockCoordinator

object Main {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("retrievers")
    val testRetriever = system.actorOf(StockCoordinator.props())

  }

}
