package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.{Actor, ActorLogging, Props, Stash}
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import com.tapiporla.microservices.retrievers.common.ElasticDAO.{DataRetrieved, ErrorRetrievingData, RetrieveAllFromIndexSorted}
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.MMDefition
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35StatManager.{CheckReadyToStart, InitIbex35StatManager, UpdateStats}
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ESDAO
import com.tapiporla.microservices.retrievers.indices.ibex35.model.Ibex35Stat
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Ibex35StatManager {
  object InitIbex35StatManager //Called after Ibex35HistoricManager is updated to avoid race conditions
  object UpdateStats //Called to Update stats (after Ibex35HistoricManager has inserted new data)
  object CheckReadyToStart //Called to check if we have all the data needed to Start the manager
}

/**
  * In charge of create stats on demand from the index
  */
class Ibex35StatManager extends Actor with ActorLogging with Stash {

  val esDAO =
    context.actorOf(Props[Ibex35ESDAO], name = "Ibex35ESDAO_Manager")

  override def receive: Receive = initial

  def initial: Receive = {

    case InitIbex35StatManager =>
      log.info("Recovering initial status")

      //Recover MM***
      esDAO ! retrieveLastMMUpdated(StatsGenerator.MM200)
      esDAO ! retrieveLastMMUpdated(StatsGenerator.MM100)
      esDAO ! retrieveLastMMUpdated(StatsGenerator.MM40)
      esDAO ! retrieveLastMMUpdated(StatsGenerator.MM20)
      context.become(gettingInitialStatus((false, None), (false, None), (false, None), (false, None)))

    case _ =>
      stash()
  }

  type CheckedMM = (Boolean, Option[DateTime])

  def gettingInitialStatus(
                          lastMM200: CheckedMM,
                          lastMM100: CheckedMM,
                          lastMM40: CheckedMM,
                          lastMM20: CheckedMM
                          ): Receive = {

    case DataRetrieved(_, _, data, rq) if rq.where.nonEmpty =>
      rq.where.get match {
        case TermQueryDefinition(Ibex35ESDAO.Stats.statsAttr, StatsGenerator.MM200._1, _, _) =>
            context.become(
                gettingInitialStatus(generateCheckedMM(data), lastMM100, lastMM40, lastMM20)
              )

        case TermQueryDefinition(Ibex35ESDAO.Stats.statsAttr, StatsGenerator.MM100._1, _, _) =>
          context.become(
            gettingInitialStatus(lastMM200, generateCheckedMM(data), lastMM40, lastMM20)
          )

        case TermQueryDefinition(Ibex35ESDAO.Stats.statsAttr, StatsGenerator.MM40._1, _, _) =>
          context.become(
            gettingInitialStatus(lastMM200, lastMM100, generateCheckedMM(data), lastMM20)
          )

        case TermQueryDefinition(Ibex35ESDAO.Stats.statsAttr, StatsGenerator.MM20._1, _, _) =>
          context.become(
            gettingInitialStatus(lastMM200, lastMM100, lastMM40, generateCheckedMM(data))
          )

        case _ => log.error("Unknown where to threat!!")
      }
      self ! CheckReadyToStart


    case ErrorRetrievingData(ex, index, typeName, rq) =>
      log.error(s"Can't get stat for init from $index / $typeName, retrying in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, esDAO, rq)

    case CheckReadyToStart =>
      if(lastMM200._1 && lastMM100._1 && lastMM40._1 && lastMM20._1){
        unstashAll()
        val retrievedDates = lastMM200._2 ++ lastMM100._2 ++ lastMM40._2 ++ lastMM20._2 toSeq

        //TODO: Remove inconsistent stats
        if(retrievedDates.length == 4) {
          val consistentDate = lastConsistentDate(retrievedDates)
          log.info(s"Recovered lastUpdate: $consistentDate, starting to listen")
          self ! UpdateStats //Autostart creating stats
          context.become(ready(Some(consistentDate)))
        } else {
          log.info("No consistent date found. Starting creating stats from scratch")
          self ! UpdateStats
          context.become(ready(None))
        }

      } else
        log.info(s"Not ready to start. Stats recovered: MM200:${lastMM200._1} MM100:${lastMM100._1} MM40:${lastMM40._1} MM20:${lastMM20._1}")


    case _ =>
      stash()

  }

  def ready(lastUpdated: Option[DateTime]): Receive = {

    case UpdateStats =>
      log.info(s"Time to update stats from Ibex35")
      esDAO ! retrieveIbexHistoricFromDate(lastUpdated)
      context.become(updating(lastUpdated, Seq()))

    case _ =>
      log.error("Unknown message while ready")
  }

  def updating(lastUpdated: Option[DateTime], lastPackageProcess: Seq[Ibex35Stat]): Receive = {

    case DataRetrieved(index, typeName, data, rq) =>
      log.info(s"Get from $index / $typeName data: ${data.hits.length} documents")


    case ErrorRetrievingData(ex, index, typeName, rq) =>
      log.error(s"Can't get data from $index / $typeName, retrying in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, self, rq)

    case _ =>
      log.error("Unknown message while updating")
  }


  import com.sksamuel.elastic4s.ElasticDsl._

  private val CHUNKS_SIZE = StatsGenerator.START_ELEMENTS_RECOMMENDED
  /**
    * To avoid too much data from Database, we are going to process by steps the stats
    * @param date
    */
  private def retrieveIbexHistoricFromDate(date: Option[DateTime]) =
    //TODO: Should the other actor retrieve data for us instead of going to ES directly??
    RetrieveAllFromIndexSorted(
      Ibex35ESDAO.index,
      Ibex35ESDAO.Historic.typeName,
      None,
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
  private def retrieveLastMMUpdated(mm: MMDefition): RetrieveAllFromIndexSorted =
    RetrieveAllFromIndexSorted(
      Ibex35ESDAO.index,
      Ibex35ESDAO.Stats.typeName,
      Some(termQuery(Ibex35ESDAO.Stats.statsAttr, mm._1)),
      Some(fieldSort(Ibex35ESDAO.date).order(SortOrder.DESC)),
      Some(1)
    )

  /**
    * Get the date where all the stats were consistent
    * @param dates
    * @return
    */
  private def lastConsistentDate(dates: Seq[DateTime]): DateTime =
    new DateTime(dates.map(_.toDate).min)

  private def generateCheckedMM(rs: RichSearchResponse): CheckedMM =
    if(rs.hits.length > 0)
      (true, Some(Ibex35Stat.fromHit(rs.hits.head).date))
    else
      (true, None)


}
