package com.tapiporla.microservices.wallet.common

import akka.actor.Stash
import akka.pattern.pipe
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import com.sksamuel.elastic4s.searches.sort.FieldSortDefinition
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import com.tapiporla.microservices.wallet.CausedBy
import com.tapiporla.microservices.wallet.common.ElasticDAO._
import com.tapiporla.microservices.wallet.common.model.ElasticDocumentInsertable
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.common.settings.Settings

import scala.concurrent.ExecutionContext.Implicits.global

object ElasticDAO {

  val documentsId = "_id"

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

  case class Update(index: String, typeName: String, id: String, newDoc: ElasticDocumentInsertable)

  case class UpdateConfirmation(index: String, typeName: String, originalRQ: Update)

  case class ErrorUpdatingData(error: Exception, index: String, typeName: String, originalRQ: Update)

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


trait ElasticDAO extends TapiporlaActor with Stash {

  def indexCreation: CreateIndexDefinition //Index to be created (if already exists we don't check if the mappings are similar)

  override val daemonTimeBeforeRetries = TapiporlaConfig.ElasticSearch.timeBeforeRetries

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

      } pipeTo self


    case ProblemConnectingWithES(e) =>
      log.error(s"Error initializing ElasticSearch DAO. Trying again in $daemonTimeBeforeRetries", e)
      context.system.scheduler.scheduleOnce(daemonTimeBeforeRetries, self, InitElasticDAO)


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
          log.error(s"Impossible to save in index $index $typeName ${jsonDocs.length} docs", e)
          ErrorSavingData(e, index, typeName, jsonDocs)
      } pipeTo sender

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
          log.error(s"Impossible to retrieve from index $index $typeName all sorted", e)
          ErrorRetrievingData(e, index, typeName, rq)
      } pipeTo sender

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
          log.error(s"Impossible to delete in index $index $typeName", e)
          ErrorDeletingData(e, index, typeName, rq)
      } pipeTo sender

    case rq @ Update(index, typeName, id, newDoc) =>
      client.execute {
        update(id) in index/typeName doc newDoc.json
      } map { _ =>
        log.info(s"Updated document with id $id, in $index $typeName, newDoc: $newDoc")
        UpdateConfirmation(index, typeName, rq)
      } recover {
        case e: Exception =>
          log.error(s"Impossible to update in index $index $typeName doc $newDoc with id $id",e)
          ErrorUpdatingData(e, index, typeName, rq)
      }
  }

}

