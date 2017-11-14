package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.{Actor, ActorLogging, Props, Stash}
import com.sksamuel.elastic4s.ElasticDsl.termQuery
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import com.tapiporla.microservices.retrievers.common.TapiporlaActor
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.MMDefinition
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35StatManager._
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ESDAO
import com.tapiporla.microservices.retrievers.indices.ibex35.model.{Ibex35Historic, Ibex35Stat}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Ibex35StatManager {
  case object InitIbex35StatManager //Called after Ibex35HistoricManager is updated to avoid race conditions
  case object UpdateStats //Called to Update stats (after Ibex35HistoricManager has inserted new data)
  case object CheckReadyToStart //Called to check if we have all the data needed to Start the manager
  case object StatsUpdatedSuccessfully //When stats has been generated succesfully

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
    Seq(200, 100, 40, 20) map StatsGenerator.MMDefinition.from //TODO: To config

}

/**
  * In charge of create stats on demand from the index
  *
  * The flow is: initial -> gettingInitialStatus -> waitingForDeletions -> ready -> updating -> ready...
  *
  */
class Ibex35StatManager extends TapiporlaActor with Stash {

  val esDAO =
    context.actorOf(Props[Ibex35ESDAO], name = "Ibex35ESDAO_Manager")

  override def receive: Receive = initial

  def initial: Receive = {

    case InitIbex35StatManager =>
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
        case TermQueryDefinition(Ibex35ESDAO.Stats.statsAttr, mmValue: String, _, _) =>
          context.become(
            gettingInitialStatus(updateMMStatuses(statuses, data, mmValue))
          )
          self ! CheckReadyToStart
      }


    case ErrorRetrievingData(ex, index, typeName, rq) =>
      log.error(s"Can't get stat for init from $index / $typeName, retrying in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, rq)

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
      log.error(s"Can't delete data from $index / $typeName retrying in 30 seconds due to:", ex)
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, rq)

    case _ =>
      stash()
  }

  def ready(lastUpdated: Option[DateTime]): Receive = {

    case UpdateStats =>
      log.info(s"Time to update stats from Ibex35")
      esDAO ! retrieveIbexHistoricFromDate(lastUpdated)
      context.become(updating(lastUpdated, Seq()))

  }

  def updating(lastUpdated: Option[DateTime], lastPackageProcess: Seq[Ibex35Historic]): Receive = {

    case DataRetrieved(index, typeName, data, _) =>
      log.info(s"Get from $index / $typeName data: ${data.hits.length} documents")

      if(data.hits.nonEmpty) {
        val ibex35HistoricRetrieved = data.hits.map(Ibex35Historic.fromHit)

        val stats: Seq[Ibex35Stat] =
          StatsGenerator.generateStatsFor(
            ibex35HistoricRetrieved.map(_.toStatInputData),
            lastPackageProcess.map(_.toStatInputData)
          ) map Ibex35Stat.fromStat

        val updatedDate = stats.last.date

        log.info(s"Saving stats updated until: $updatedDate")
        esDAO ! SaveInIndex(Ibex35ESDAO.index, Ibex35ESDAO.Stats.typeName, stats)
        context.become(updating(Some(updatedDate), ibex35HistoricRetrieved))
      } else {
        log.error(s"No data to be updated from $index. Check if new data is being retrieved correctly. Moving to ready")
        context.become(ready(lastUpdated))
        context.parent ! StatsUpdatedSuccessfully
      }



    case ErrorRetrievingData(ex, index, typeName, rq) =>
      log.error(s"Can't get data from $index / $typeName, retrying in 30 seconds due to:", ex)
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, rq)

    case ErrorSavingData(ex, index, typeName, data) =>
      log.error(s"Can't save data in $index / $typeName, retrying in 30 seconds:", ex)
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, SaveInIndex(index,typeName,data))

    case DataSavedConfirmation(index, typeName, data) =>
      log.info(s"Saved stats correctly in ES ($index / $typeName): ${data.length} documents")
      if(data.length < CHUNKS_SIZE) {
        log.info(s"Being ready with date of stats generated: $lastUpdated")
        context.become(ready(lastUpdated))
        context.parent ! StatsUpdatedSuccessfully
      } else { //Continue generating Stats
        log.info(s"We have more data to generate stats. Continue updating stats from: $lastUpdated")
        esDAO ! retrieveIbexHistoricFromDate(lastUpdated)
      }

  }


  import com.sksamuel.elastic4s.ElasticDsl._

  private val CHUNKS_SIZE = StatsGenerator.START_ELEMENTS_RECOMMENDED
  /**
    * To avoid too much data from Database, we are going to process by steps the stats
    * @param date
    */
  private def retrieveIbexHistoricFromDate(date: Option[DateTime]) =

    RetrieveAllFromIndexSorted(
      Ibex35ESDAO.index,
      Ibex35ESDAO.Historic.typeName,
      date map {dt => rangeQuery(Ibex35ESDAO.date) gt dt.toString},
      Some(fieldSort(Ibex35ESDAO.date).order(SortOrder.ASC)),
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
      Ibex35ESDAO.index,
      Ibex35ESDAO.Stats.typeName,
      Some(termQuery(Ibex35ESDAO.Stats.statsAttr, mm.identifier)),
      Some(fieldSort(Ibex35ESDAO.date).order(SortOrder.DESC)),
      Some(1)
    )

  /**
    * Generate messages of deletion
    * @param date
    * @return
    */
  private def deleteStatsOlderThan(date: Option[DateTime]): DeleteFromIndex = {
    val deleteQuery = date map { consistentDate =>
      rangeQuery(Ibex35ESDAO.date) gt consistentDate.toString
    }

    DeleteFromIndex(
      Ibex35ESDAO.index,
      Ibex35ESDAO.Stats.typeName,
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
    if(rs.hits.length > 0)
      status.checked(Some(Ibex35Stat.fromHit(rs.hits.head).date))
    else
      status.checked(None)


}
