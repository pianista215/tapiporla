package com.tapiporla.microservices.retrievers.common

import akka.actor.{Actor, ActorLogging, Stash}
import akka.pattern.pipe
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import com.sksamuel.elastic4s.searches.sort.FieldSortDefinition
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.common.settings.Settings

import scala.concurrent.ExecutionContext.Implicits.global

object ElasticDAO {

  case class SaveInIndex(index: String, typeName: String, jsonDocs: Seq[String])

  case class ErrorSavingData(
                              error: Exception,
                              index: String,
                              typeName: String,
                              jsonDocs: Seq[String]
                            )

  case class DataSavedConfirmation(index: String, typeName: String, jsonDocs: Seq[String])

  case class RetrieveAllFromIndexSorted(
                                         index: String,
                                         typeName: String,
                                         where: Option[QueryDefinition] = None,
                                         sort: Option[FieldSortDefinition] = None,
                                         limit: Option[Int] = None)

  case class DataRetrieved(
                            index: String,
                            typeName: String,
                            data: RichSearchResponse,
                            originalRQ: RetrieveAllFromIndexSorted
                          )

  case class ErrorRetrievingData(
                                  error: Exception,
                                  index: String,
                                  typeName: String,
                                  originalRQ: RetrieveAllFromIndexSorted
                                )

  object InitElasticDAO
  object ReadyToListen

  object CausedBy { //TODO: Move to package.scala
    def unapply(e: Throwable): Option[Throwable] = Option(e.getCause)
  }

  //TODO: To settings
  val settings = Settings.builder().put("cluster.name", "docker-cluster").put("xpack.security.user", "elastic:changeme").build()
  val client = XPackElasticClient(settings, ElasticsearchClientUri("elasticsearch://localhost:9300"))

}


trait ElasticDAO extends Actor with ActorLogging with Stash{

  def indexCreation: CreateIndexDefinition //Index to be created (if already exists we don't check if the mappings are similar)

  self ! InitElasticDAO

  override def receive = initial

  def initial: Receive = {

    case InitElasticDAO =>
      client.execute(indexCreation) map { _ =>
        ReadyToListen
      } recover {
        case CausedBy(ex: ResourceAlreadyExistsException) => //TODO: Change to see how to check if index already exists
          ReadyToListen
        case e: Exception =>
          log.error(s"Error initializing ElasticSearch DAO ${self.path.name} :$e. Trying again")
          InitElasticDAO
      } pipeTo (self)


    case ReadyToListen =>
      unstashAll()
      context.become(readyToProcess)

    case _ =>
      stash()
  }


  def readyToProcess: Receive = {

    case SaveInIndex(index, typeName, jsonDocs) =>
      client.execute {
        bulk (
          jsonDocs map (indexInto(index / typeName).doc(_))
        ) refresh(RefreshPolicy.WAIT_UNTIL)
      } map { _=>
        log.info(s"Completed to save in ES index $index type $typeName: ${jsonDocs.length} docs")
        DataSavedConfirmation(index, typeName, jsonDocs)
      } recover {
        case e: Exception =>
          log.error(s"Impossible to save in index $index $typeName ${jsonDocs.length} docs: $e")
          ErrorSavingData(e, index, typeName, jsonDocs)
      } pipeTo(sender)

    case rq @ RetrieveAllFromIndexSorted(index, typeName, where, sort, limitValue) =>
      client.execute {
        where map { whereQuery =>
          search(index / typeName) query whereQuery sortBy sort limit (limitValue.getOrElse(0))
        } getOrElse
          search(index / typeName) sortBy sort limit (limitValue.getOrElse(0))

      } map { result =>
        log.info(s"Retrieved from ES ${result.hits.length} elements")
        DataRetrieved(index, typeName, result, rq)
      } recover {
        case e: Exception =>
          log.error(s"Impossible to retrieve from index $index $typeName all sorted: $e")
          ErrorRetrievingData(e, index, typeName, rq)
      } pipeTo(sender)

    case _ =>
      log.error("Unknown message")
  }

}
