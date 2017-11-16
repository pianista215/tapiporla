package com.tapiporla.microservices.retrievers.indices.stock

import akka.actor.{Props, Stash}
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.MMDefinition
import com.tapiporla.microservices.retrievers.common.{TapiporlaActor, TapiporlaConfig}
import com.tapiporla.microservices.retrievers.indices.stock.StockStatManager._
import com.tapiporla.microservices.retrievers.indices.stock.dao.StockESDAO
import com.tapiporla.microservices.retrievers.indices.stock.model.{StockHistoric, StockStat}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global

object StockStatManager {
  case object InitStatManager //Called after StockHistoricManager is updated to avoid race conditions
  case object UpdateStats //Called to Update stats (after StockHistoricManager has inserted new data)
  case object CheckReadyToStart //Called to check if we have all the data needed to Start the manager
  case object StatsUpdatedSuccessfully //When stats has been generated succesfully

  def props(): Props = Props(new StockStatManager())

  object MMStatus {
    def unchecked(mm: MMDefinition): MMStatus = MMStatus(mm, false, None)
  }

  case class MMStatus(mm: MMDefinition, checked: Boolean, lastUpdate: Option[DateTime]) {

    def checked(updatedDate: Option[DateTime]) =
      copy(lastUpdate = updatedDate, checked = true)

    override def toString: String =
      s"${mm.identifier} checked: $checked"

  }


  val MMToCollect: Seq[MMDefinition] =
    TapiporlaConfig.Stats.mmGeneration map StatsGenerator.MMDefinition.from

}

/**
  * In charge of create stats on demand from the index
  *
  * The flow is: initial -> gettingInitialStatus -> waitingForDeletions -> ready -> updating -> ready...
  *
  */
class StockStatManager extends TapiporlaActor with Stash {

  val esDAO =
    context.actorOf(StockESDAO.props(), name = s"${stockName}ESDAO_Manager")

  override def receive: Receive = initial

  def initial: Receive = {

    case InitStatManager =>
      log.info("Recovering initial status")

      //Recover MM*** status
      MMToCollect foreach { mm =>
        esDAO ! retrieveLastMMUpdated(mm)
      }

      context.become(gettingInitialStatus(MMToCollect map (mm => MMStatus.unchecked(mm))))

    case _ =>
      stash()
  }

  def gettingInitialStatus(
                            statuses: Seq[MMStatus]
                          ): Receive = {

    case DataRetrieved(_, _, data, rq) if rq.where.nonEmpty => rq.where.get match {
        case TermQueryDefinition(StockESDAO.Stats.statsAttr, mmValue: String, _, _) =>
          context.become(
            gettingInitialStatus(updateMMStatuses(statuses, data, mmValue))
          )
          self ! CheckReadyToStart
      }


    case ErrorRetrievingData(ex, index, typeName, rq) =>
      log.error(s"Can't get stat for init from $index / $typeName, retrying in $daemonTimeBeforeRetries")
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, esDAO, rq)

    case CheckReadyToStart =>

      if(statuses.forall(_.checked)){
        unstashAll()
        val retrievedDates: Seq[DateTime] = statuses.flatMap(_.lastUpdate)

        log.info("Retrieved last updated fields")

        val consistentDate =
          if(retrievedDates.length == MMToCollect.length)
            Some(lastConsistentDate(retrievedDates))
          else
            None

        //Remove inconsistent data
        log.info(s"Starting deletions of inconsistent data from: $consistentDate")
        esDAO ! deleteStatsOlderThan(consistentDate)
        context.become(waitingForDeletions(consistentDate))

      } else
        log.info(s"Not ready to start. Stats recovered: $statuses")


    case _ =>
      stash()

  }

  def waitingForDeletions(consistentDate: Option[DateTime]): Receive = {

    case DeleteConfirmation(index, typeName, _) =>
      log.info(s"Successfully removed inconsistent Stats in $index / $typeName, newer than: $consistentDate")
      self ! UpdateStats
      context.become(ready(consistentDate))

    case ErrorDeletingData(ex, index, typeName, rq) =>
      log.error(s"Can't delete data from $index / $typeName retrying in $daemonTimeBeforeRetries due to:", ex)
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, esDAO, rq)

    case _ =>
      stash()
  }

  def ready(lastUpdated: Option[DateTime]): Receive = {

    case UpdateStats =>
      log.info(s"Time to update stats from $stockName")

      lastUpdated.fold {
        //No previous historic data
        log.info("Starting stats from Scratch")
        esDAO ! retrieveStockHistoricFromDate(lastUpdated)
        context.become(updating(lastUpdated, Seq()))
      }{ _ =>
        log.info(s"Retrieving last historic data, to start computing stats from the date $lastUpdated")
        //Retrieve last historic retrieved, to compute stats in next iteration
        esDAO ! retrieveStockHistoricUntilDate(lastUpdated)
      }


    //We need to retrieve previous historic data, to compute the stats in next step
    case DataRetrieved(_, _, data, _) =>
        log.info(s"Retrieved previous historic data from $lastUpdated: ${data.hits.length} data")
        esDAO ! retrieveStockHistoricFromDate(lastUpdated)
        context.become(updating(lastUpdated, data.hits.map(StockHistoric.fromHit).reverse))


    case ErrorRetrievingData(ex, index, typeName, rq) =>
      log.error(s"Can't get stats until $lastUpdated from $index / $typeName, retrying in $daemonTimeBeforeRetries")
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, esDAO, rq)

  }

  def updating(lastUpdated: Option[DateTime], lastPackageProcess: Seq[StockHistoric]): Receive = {

    case DataRetrieved(index, typeName, data, _) =>
      log.info(s"Get from $index / $typeName data: ${data.hits.length} documents")

      if(data.hits.nonEmpty) {
        val stockHistoricRetrieved = data.hits.map(StockHistoric.fromHit)

        val stats: Seq[StockStat] =
          StatsGenerator.generateMultipleMMs(
            stockHistoricRetrieved.map(_.toStatInputData),
            lastPackageProcess.map(_.toStatInputData),
            MMToCollect
          ) map StockStat.fromStat
        

        val updatedDate = stats.last.date

        log.info(s"Saving stats updated until: $updatedDate")
        esDAO ! SaveInIndex(StockESDAO.index, StockESDAO.Stats.typeName, stats)
        context.become(updating(Some(updatedDate), stockHistoricRetrieved))
      } else {
        log.error(s"No data to be updated from $index. Check if new data is being retrieved correctly. Moving to ready")
        context.become(ready(lastUpdated))
        context.parent ! StatsUpdatedSuccessfully
      }



    case ErrorRetrievingData(ex, index, typeName, rq) =>
      log.error(s"Can't get data from $index / $typeName, retrying in $daemonTimeBeforeRetries due to:", ex)
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, esDAO, rq)

    case ErrorSavingData(ex, index, typeName, data) =>
      log.error(s"Can't save data in $index / $typeName, retrying in $daemonTimeBeforeRetries:", ex)
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, esDAO, SaveInIndex(index,typeName,data))

    case DataSavedConfirmation(index, typeName, data) =>
      log.info(s"Saved stats correctly in ES ($index / $typeName): ${data.length} documents")
      if(data.length < CHUNKS_SIZE) {
        log.info(s"Being ready with date of stats generated: $lastUpdated")
        context.become(ready(lastUpdated))
        context.parent ! StatsUpdatedSuccessfully
      } else { //Continue generating Stats
        log.info(s"We have more data to generate stats. Continue updating stats from: $lastUpdated")
        esDAO ! retrieveStockHistoricFromDate(lastUpdated)
      }

  }


  import com.sksamuel.elastic4s.ElasticDsl._

  private val CHUNKS_SIZE = MMToCollect.maxBy(_.numberOfItems).numberOfItems * 2

  /**
    * To avoid too much data from Database, we are going to process by steps the stats
    * @param date
    */
  private def retrieveStockHistoricFromDate(date: Option[DateTime]) =

    RetrieveAllFromIndexSorted(
      StockESDAO.index,
      StockESDAO.Historic.typeName,
      date map {dt => rangeQuery(StockESDAO.date) gt dt.toString},
      Some(fieldSort(StockESDAO.date).order(SortOrder.ASC)),
      Some(CHUNKS_SIZE)
    )

  /**
    * Retrieve last historic data until the date, to start computing stats for new Data from crawler
    * @param date
    */
  private def retrieveStockHistoricUntilDate(date: Option[DateTime]) =

    RetrieveAllFromIndexSorted(
      StockESDAO.index,
      StockESDAO.Historic.typeName,
      date map {dt => rangeQuery(StockESDAO.date) lte dt.toString},
      Some(fieldSort(StockESDAO.date).order(SortOrder.DESC)),
      Some(CHUNKS_SIZE)
    )

  /**
    * Generating messages to retrieve last generated stat of each type
    * If we found that the dates are not matching, some error happened in the last insertion
    * So we delete the data before the "consistent" date and reprocess them
    * @param mm
    * @return
    */
  private def retrieveLastMMUpdated(mm: MMDefinition): RetrieveAllFromIndexSorted =
    RetrieveAllFromIndexSorted(
      StockESDAO.index,
      StockESDAO.Stats.typeName,
      Some(termQuery(StockESDAO.Stats.statsAttr, mm.identifier)),
      Some(fieldSort(StockESDAO.date).order(SortOrder.DESC)),
      Some(1)
    )

  /**
    * Generate messages of deletion
    * @param date
    * @return
    */
  private def deleteStatsOlderThan(date: Option[DateTime]): DeleteFromIndex = {
    val deleteQuery = date map { consistentDate =>
      rangeQuery(StockESDAO.date) gt consistentDate.toString
    }

    DeleteFromIndex(
      StockESDAO.index,
      StockESDAO.Stats.typeName,
      deleteQuery
    )
  }

  /**
    * Get the date where all the stats were consistent
    * @param dates
    * @return
    */
  private def lastConsistentDate(dates: Seq[DateTime]): DateTime =
    new DateTime(dates.map(_.toDate).min)

  private def updateMMStatuses(
                             oldStatuses: Seq[MMStatus],
                             esResponse: RichSearchResponse,
                             mmFoundFilter: String
                            ): Seq[MMStatus] = {

    oldStatuses map { status =>
      if(status.mm.identifier == mmFoundFilter)
        generateCheckedMM(status, esResponse)
      else
        status
    }

  }

  private def generateCheckedMM(status: MMStatus, rs: RichSearchResponse): MMStatus =
    if(rs.hits.nonEmpty)
      status.checked(Some(StockStat.fromHit(rs.hits.head).date))
    else
      status.checked(None)


}
