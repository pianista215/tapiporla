package com.tapiporla.microservices.retrievers.indices.stock

import akka.actor.{Props, Stash}
import com.tapiporla.microservices.retrievers.common.{ElasticDAO, Retriever, TapiporlaConfig}
import com.tapiporla.microservices.retrievers.common.Retriever.UpdateHistoricData
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import com.tapiporla.microservices.retrievers.indices.stock.StockHistoricManager.{InitCoordinator, ReadyToStart, UpdateComplete}
import com.tapiporla.microservices.retrievers.indices.stock.dao.StockScrapyDAO.{CantRetrieveDataFromCrawler, StockDataRetrieved, RetrieveAllStockData, RetrieveStockDataFrom}
import com.tapiporla.microservices.retrievers.indices.stock.dao.{StockESDAO, StockScrapyDAO}
import com.tapiporla.microservices.retrievers.indices.stock.model.StockHistoric
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



object StockHistoricManager {
  case object InitCoordinator
  case class ReadyToStart(lastUpdated: Option[DateTime])
  case object UpdateComplete
}

/**
  * Actor in charge of the Stock data retrieved from ScrapyRT
  * and persisted in ElasticSearch
  */
class StockHistoricManager extends Retriever with Stash {

  val esDAO =
    context.actorOf(Props[StockESDAO], name = s"${stockName}ESDAO_Historic")

  val scrapyDAO =
    context.actorOf(Props[StockScrapyDAO], name = s"${stockName}ScrapyDAO_Historic")

  override def preStart() =
    self ! InitCoordinator

  override def receive = initial

  def initial: Receive = {

    case InitCoordinator =>
      import com.sksamuel.elastic4s.ElasticDsl._
      log.info("Recovering initial status")
      esDAO ! RetrieveAllFromIndexSorted(
        StockESDAO.index,
        StockESDAO.Historic.typeName,
        None,
        Some(fieldSort(StockESDAO.date).order(SortOrder.DESC)),
        Some(1)
      )

    case DataRetrieved(index, typeName, data, _) =>
      data.hits.headOption.fold {
        log.info("Starting without initial date")
        self ! ReadyToStart(None)
      } { _ =>
        log.info("Starting with initial retrieved")
        self ! ReadyToStart(Some(StockHistoric.fromHit(data.hits.head).date))
      }

    case ErrorRetrievingData(ex, index, typeName, originalRQ) =>
      log.error(s"Can't get initial status from $index / $typeName, retrying in $daemonTimeBeforeRetries")
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, esDAO, originalRQ)


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
      log.info(s"Time to update historic data from $stockName")
      lastUpdated map { date =>
        scrapyDAO ! RetrieveStockDataFrom(date)
      } getOrElse
        scrapyDAO ! RetrieveAllStockData

    case StockDataRetrieved(historicData) =>
      log.info(s"Data retrieved from $stockName to be updated ${historicData.length}")

      if(historicData.nonEmpty) {
        log.info(s"Sending to ES new Data")
        esDAO ! SaveInIndex(
          StockESDAO.index,
          StockESDAO.Historic.typeName,
          historicData
        )
        val lastDate = historicData.maxBy(_.date.toString).date
        log.info(s"LastUpdated is now $lastDate")
        context.become(ready(Some(lastDate)))
      } else
        context.parent ! UpdateComplete

    case CantRetrieveDataFromCrawler =>
      log.info(s"Cant retrieve from Crawler $stockName data, will try in $daemonTimeBeforeRetries")
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, self, UpdateHistoricData)

    case ErrorSavingData(ex, index, typeName, data) =>
      //We are just saving one by one, never two SaveOperations are happening in the same actor at the same time
      log.info(s"Retrying to save ES in $index / $typeName after $ex in $daemonTimeBeforeRetries")
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, esDAO, SaveInIndex(index, typeName, data))

    case DataSavedConfirmation(index, typeName, data) =>
      log.info(s"Data saved. Calling father to update stats")
      context.parent ! UpdateComplete

  }

}
