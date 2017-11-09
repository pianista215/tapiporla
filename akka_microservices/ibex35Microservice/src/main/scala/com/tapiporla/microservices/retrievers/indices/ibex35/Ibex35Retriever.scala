package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.{Props, Stash}
import com.tapiporla.microservices.retrievers.common.{ElasticDAO, Retriever}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateData
import Ibex35CrawlerAPI.{CantRetrieveDataFromIbex35Crawler, IbexDataRetrieved, RetrieveAllIbexData, RetrieveIbexDataFrom}
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35Retriever.{InitIbex35Retriever, ReadyToStart}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



object Ibex35Retriever {
  object InitIbex35Retriever
  case class ReadyToStart(lastUpdated: Option[DateTime])
}

/**
  * Actor in charge of the Ibex35 data retrieved from ScrapyRT
  * and persisted in ElasticSearch
  */
class Ibex35Retriever extends Retriever with Stash {

  val esDAO =
    context.actorOf(Props[Ibex35ESDAO], name = "Ibex35ESDAO")

  val apiDAO =
    context.actorOf(Props[Ibex35CrawlerAPI], name = "Ibex35CrawlerAPI")

  self ! InitIbex35Retriever

  override def receive = initial

  def initial: Receive = {

    case InitIbex35Retriever =>
      import com.sksamuel.elastic4s.ElasticDsl._
      log.info("Recovering initial status")
      esDAO ! RetrieveAllFromIndexSorted(Some(fieldSort(Ibex35ESDAO.date).order(SortOrder.DESC)), Some(1))

    case DataRetrieved(index, typeName, data) =>
      if(data.isEmpty) {
        log.info("Starting without initial date")
        self ! ReadyToStart(None)
      } else {
        log.info("Starting with initial retrieved")
        self ! ReadyToStart(Some(Ibex35Historic.fromHit(data.hits.head).date))
      }

    case ErrorRetrievingData(ex, index, typeName) =>
      log.error("Can't get initial status, retrying in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, self, InitIbex35Retriever)


    case ReadyToStart(lastUpdated) =>
      unstashAll()
      log.info(s"Recovered lastUpdate: $lastUpdated, starting to listen")
      context.become(ready(lastUpdated))

    case _ =>
      stash()
  }

  def ready(lastUpdated: Option[DateTime]): Receive = {

    case UpdateData =>
      lastUpdated map { date =>
        apiDAO ! RetrieveIbexDataFrom(date)
      } getOrElse
        apiDAO ! RetrieveAllIbexData

    case IbexDataRetrieved(historicData) =>
      log.info(s"Data retrieved from Ibex35 to be updated ${historicData.length}")

      if(!historicData.isEmpty) {
        log.info(s"Sending to ES new Data")
        esDAO ! SaveInIndex(historicData.map(Ibex35Historic.json))
        val lastDate = historicData.sortBy(_.date.toString).last.date
        log.info(s"LastUpdated is now $lastDate")
        context.become(ready(Some(lastDate)))
      }

    case CantRetrieveDataFromIbex35Crawler =>
      log.info("Cant retrieve from Crawler Ibex35 data, will try in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, self, UpdateData)

    case ErrorSavingData(ex, index, typeName, data) =>
      log.info(s"Retrying to save ES data after $ex in 30 seconds") //TODO: CQRS????
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, SaveInIndex(data))

    case DataSavedConfirmation(_,_,_) => //Ignore

    case _ =>
      log.error("Unkown message") //TODO: Now is getting message from ESDAO?????
  }

}
