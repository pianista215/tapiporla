package com.tapiporla.microservices.retrievers.indices.common.ibex35

import akka.actor.Props
import com.tapiporla.microservices.retrievers.common.{ElasticDAO, Retriever}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateData
import com.tapiporla.microservices.retrievers.indices.common.ibex35.Ibex35CrawlerAPI.RetrieveAllIbexData

/**
  * Actor in charge of the Ibex35 data retrieved from ScrapyRT
  * and persisted in ElasticSearch
  */
class Ibex35Retriever extends Retriever{

  val esDAO =
    context.actorOf(Props[ElasticDAO], name = "Ibex35ESDAO")

  val apiDAO =
    context.actorOf(Props[Ibex35CrawlerAPI], name = "Ibex35CrawlerAPI")


  override def receive = {

    case UpdateData =>
      updateData()

    case _ =>
      log.error("Unkown message")
  }


  private def updateData(): Unit = {
    apiDAO ! RetrieveAllIbexData
  }
}
