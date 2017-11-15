package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.{Props, Stash}
import com.tapiporla.microservices.retrievers.common.{ElasticDAO, Retriever}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateHistoricData
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35HistoricManager.{InitIbex35Coordinator, ReadyToStart, UpdateComplete}
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ScrapyDAO.{CantRetrieveDataFromIbex35Crawler, IbexDataRetrieved, RetrieveAllIbexData, RetrieveIbexDataFrom}
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.{Ibex35ESDAO, Ibex35ScrapyDAO}
import com.tapiporla.microservices.retrievers.indices.ibex35.model.Ibex35Historic
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



object Ibex35HistoricManager {
  case object InitIbex35Coordinator
  case class ReadyToStart(lastUpdated: Option[DateTime])
  case object UpdateComplete
}

/**
  * Actor in charge of the Ibex35 data retrieved from ScrapyRT
  * and persisted in ElasticSearch
  */
class Ibex35HistoricManager extends Retriever with Stash {

  val esDAO =
    context.actorOf(Props[Ibex35ESDAO], name = "Ibex35ESDAO_Historic")

  val scrapyDAO =
    context.actorOf(Props[Ibex35ScrapyDAO], name = "Ibex35ScrapyDAO_Historic")

  override def preStart() =
    self ! InitIbex35Coordinator

  override def receive = initial

  def initial: Receive = {

    case InitIbex35Coordinator =>
      import com.sksamuel.elastic4s.ElasticDsl._
      log.info("Recovering initial status")
      esDAO ! RetrieveAllFromIndexSorted(
        Ibex35ESDAO.index,
        Ibex35ESDAO.Historic.typeName,
        None,
        Some(fieldSort(Ibex35ESDAO.date).order(SortOrder.DESC)),
        Some(1)
      )

    case DataRetrieved(index, typeName, data, _) =>
      data.hits.headOption.fold {
        log.info("Starting without initial date")
        self ! ReadyToStart(None)
      } { _ =>
        log.info("Starting with initial retrieved")
        self ! ReadyToStart(Some(Ibex35Historic.fromHit(data.hits.head).date))
      }

    case ErrorRetrievingData(ex, index, typeName, originalRQ) =>
      log.error(s"Can't get initial status from $index / $typeName, retrying in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, originalRQ)


    case ReadyToStart(lastUpdated) =>
      unstashAll()
      log.info(s"Recovered lastUpdate: $lastUpdated, starting to listen")
      self ! UpdateHistoricData //Auto start updating data
      context.become(ready(lastUpdated))

    case _ =>
      stash()
  }

  def ready(lastUpdated: Option[DateTime]): Receive = {

    case UpdateHistoricData =>
      log.info(s"Time to update historic data from Ibex35")
      lastUpdated map { date =>
        scrapyDAO ! RetrieveIbexDataFrom(date)
      } getOrElse
        scrapyDAO ! RetrieveAllIbexData

    case IbexDataRetrieved(historicData) =>
      log.info(s"Data retrieved from Ibex35 to be updated ${historicData.length}")

      if(historicData.nonEmpty) {
        log.info(s"Sending to ES new Data")
        esDAO ! SaveInIndex(
          Ibex35ESDAO.index,
          Ibex35ESDAO.Historic.typeName,
          historicData
        )
        val lastDate = historicData.maxBy(_.date.toString).date
        log.info(s"LastUpdated is now $lastDate")
        context.become(ready(Some(lastDate)))
      } else
        context.parent ! UpdateComplete

    case CantRetrieveDataFromIbex35Crawler =>
      log.info("Cant retrieve from Crawler Ibex35 data, will try in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, self, UpdateHistoricData)

    case ErrorSavingData(ex, index, typeName, data) =>
      //We are just saving one by one, never two SaveOperations are happening in the same actor at the same time
      log.info(s"Retrying to save ES in $index / $typeName after $ex in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, SaveInIndex(index, typeName, data))

    case DataSavedConfirmation(index, typeName, data) =>
      log.info(s"Data saved. Calling father to update stats")
      context.parent ! UpdateComplete

  }

}
