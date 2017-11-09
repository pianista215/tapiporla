package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.Props
import com.tapiporla.microservices.retrievers.common.{ElasticDAO, Retriever}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateData
import Ibex35CrawlerAPI.{CantRetrieveDataFromIbex35, IbexDataRetrieved, RetrieveAllIbexData}
import com.tapiporla.microservices.retrievers.common.ElasticDAO.{ErrorSavingData, SaveInIndex}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Actor in charge of the Ibex35 data retrieved from ScrapyRT
  * and persisted in ElasticSearch
  */
class Ibex35Retriever extends Retriever{

  val esDAO =
    context.actorOf(Props[Ibex35ESDAO], name = "Ibex35ESDAO")

  val apiDAO =
    context.actorOf(Props[Ibex35CrawlerAPI], name = "Ibex35CrawlerAPI")


  override def receive = {

    case UpdateData =>
      apiDAO ! RetrieveAllIbexData

    case IbexDataRetrieved(historicData) =>
      log.info(s"Data retrieved from Ibex35 to be updated ${historicData.length} sending to ES")
      esDAO ! SaveInIndex(Ibex35ESDAO.index, Ibex35ESDAO.mapping, historicData.map(Ibex35ESDAO.json))

    case CantRetrieveDataFromIbex35 =>
      log.info("Cant retrieve from Crawler Ibex35 data, will try in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, self, UpdateData)

    case ErrorSavingData(ex, index, typeName, data) =>
      log.info(s"Retrying to save ES data after $ex in 30 seconds") //TODO: CQRS????
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, SaveInIndex(index, typeName, data))

    case _ =>
      log.error("Unkown message") //TODO: Now is getting message from ESDAO?????
  }

}
