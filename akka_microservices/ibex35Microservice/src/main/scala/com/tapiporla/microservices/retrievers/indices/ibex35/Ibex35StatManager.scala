package com.tapiporla.microservices.retrievers.indices.ibex35

import akka.actor.{Actor, ActorLogging, Props, Stash}
import com.tapiporla.microservices.retrievers.common.ElasticDAO.{DataRetrieved, ErrorRetrievingData, RetrieveAllFromIndexSorted}
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35StatManager.{InitIbex35StatManager, ReadyToStart, UpdateStats}
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ESDAO
import com.tapiporla.microservices.retrievers.indices.ibex35.model.Ibex35Stat
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Ibex35StatManager {
  object InitIbex35StatManager //Called after Ibex35HistoricManager is updated to avoid race conditions
  object UpdateStats //Called to Update stats (after Ibex35HistoricManager has inserted new data)
  case class ReadyToStart(lastUpdated: Option[DateTime])
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
      import com.sksamuel.elastic4s.ElasticDsl._
      log.info("Recovering initial status")
      esDAO ! RetrieveAllFromIndexSorted(
        Ibex35ESDAO.index,
        Ibex35ESDAO.Stats.typeName,
        Some(fieldSort(Ibex35ESDAO.date).order(SortOrder.DESC)),
        Some(1)
      )

    case DataRetrieved(index, typeName, data) =>
      if(data.isEmpty) {
        log.info("Starting without initial date")
        self ! ReadyToStart(None)
      } else {
        log.info("Starting with initial retrieved")
        self ! ReadyToStart(Some(Ibex35Stat.fromHit(data.hits.head).date))
      }

    case ErrorRetrievingData(ex, index, typeName) =>
      log.error(s"Can't get initial status from $index / $typeName, retrying in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, self, InitIbex35StatManager)

    case ReadyToStart(lastUpdated) =>
      unstashAll()
      log.info(s"Recovered lastUpdate: $lastUpdated, starting to listen")
      self ! UpdateStats //Autostart creating stats
      context.become(ready(lastUpdated))

    case _ =>
      stash()
  }

  def ready(lastUpdated: Option[DateTime]): Receive = {

    case UpdateStats =>

  }

}
