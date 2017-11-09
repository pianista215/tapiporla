package com.tapiporla.microservices.retrievers.common

import akka.actor.{Actor, ActorLogging, Stash}

import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.tapiporla.microservices.retrievers.common.ElasticDAO._
import akka.pattern.pipe
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import org.elasticsearch.common.settings.Settings

import scala.concurrent.duration._
import scala.concurrent.Future

object ElasticDAO {

  case class SaveInIndex(index: String, typeName: String, jsonDocs: Seq[String])
  case class RetrieveFromIndex()
  case class ErrorSavingData(error: Exception, index: String, typeName: String, jsonDocs: Seq[String])
  object InitElasticDAO
  object ReadyToListen

}


trait ElasticDAO extends Actor with ActorLogging with Stash{

  //TODO: To settings
  val settings = Settings.builder().put("cluster.name", "docker-cluster").put("xpack.security.user", "elastic:changeme").build()
  val client = XPackElasticClient(settings, ElasticsearchClientUri("elasticsearch://localhost:9300"))

  def indices: Seq[CreateIndexDefinition]

  self ! InitElasticDAO

  def receive = initial

  def initial: Receive = {

    case InitElasticDAO =>
      Future.sequence(indices map { index => client.execute(index) }) map { _ =>
        ReadyToListen
      } recover {
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
        )
      }.map { _=>
        log.info(s"Completed to save in ES index $index type $typeName: ${jsonDocs.length} docs")
      } recover {
        case e: Exception =>
          log.error(s"Impossible to save in index $index $typeName ${jsonDocs.length} docs: $e")
          ErrorSavingData(e, index, typeName, jsonDocs)
      } pipeTo(sender)

    case RetrieveFromIndex() =>

    case _ => log.error("Unknown message")
  }

}
