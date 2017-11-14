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
import com.tapiporla.microservices.retrievers.CausedBy
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import com.tapiporla.microservices.retrievers.common.model.ElasticDocumentInsertable
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.common.settings.Settings

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ElasticDAO {

  case class SaveInIndex(index: String, typeName: String, jsonDocs: Seq[ElasticDocumentInsertable])

  case class ErrorSavingData(
                              error: Exception,
                              index: String,
                              typeName: String,
                              jsonDocs: Seq[ElasticDocumentInsertable]
                            )

  case class DataSavedConfirmation(index: String, typeName: String, jsonDocs: Seq[ElasticDocumentInsertable])

  case class DeleteFromIndex(
                              index: String,
                              typeName: String,
                              where: Option[QueryDefinition] = None
                            )

  case class ErrorDeletingData(
                              error: Exception,
                              index: String,
                              typeName: String,
                              originalRQ: DeleteFromIndex
                              )

  case class DeleteConfirmation(index: String, typeName: String, originalRQ: DeleteFromIndex)

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

  case object InitElasticDAO
  case class ProblemConnectingWithES(e: Exception)
  case object ReadyToListen

  private val settings = Settings.builder()
    .put("cluster.name", TapiporlaConfig.ElasticSearch.clusterName)
    .put("xpack.security.user", TapiporlaConfig.ElasticSearch.authUser)
    .build()

  val client = XPackElasticClient(settings, ElasticsearchClientUri(TapiporlaConfig.ElasticSearch.endpoint))

}


trait ElasticDAO extends Actor with ActorLogging with Stash{

  def indexCreation: CreateIndexDefinition //Index to be created (if already exists we don't check if the mappings are similar)

  override def preStart() =
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
          ProblemConnectingWithES(e)

      } pipeTo (self)


    case ProblemConnectingWithES(e) =>
      log.error(s"Error initializing ElasticSearch DAO ${self.path.name} :$e. Trying again in 30 seconds")
      context.system.scheduler.scheduleOnce(30 seconds, self, InitElasticDAO)


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
          jsonDocs.map(x => indexInto(index / typeName).doc(x.json))
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

    case rq @ DeleteFromIndex(index, typeName, where) =>
      client.execute {

        where map { whereQuery =>
          deleteIn(index / typeName).by(whereQuery)
        } getOrElse
          deleteIn(index / typeName).by(matchAllQuery())

      } map { _ =>
        log.info(s"Deleted confirmation in ES $index / $typeName")
        DeleteConfirmation(index, typeName, rq)
      } recover {
        case e: Exception =>
          log.error(s"Impossible to delete in index $index $typeName: $e")
          ErrorDeletingData(e, index, typeName, rq)
      } pipeTo(sender)

    case _ =>
      log.error("Unknown message")
  }

}
