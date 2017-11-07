package com.tapiporla.microservices.retrievers.common

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}
import com.sksamuel.elastic4s.ElasticDsl._
import com.tapiporla.microservices.retrievers.common.ElasticDAO.{RetrieveFromIndex, SaveInIndex}

object ElasticDAO {

  case class SaveInIndex(index: String, typeName: String, jsonDocs: List[String])
  case class RetrieveFromIndex()

}


class ElasticDAO extends Actor with ActorLogging {

  //TODO: To settings
  val client = TcpClient.transport(ElasticsearchClientUri("localhost", 9300))

  def receive = {

    case SaveInIndex(index, typeName, jsonDocs) =>
      client.execute {
        bulk (
          jsonDocs map (indexInto(index / typeName).doc(_))
        )
      }.map { _=>
        log.info(s"Completed to save in ES index $index type $typeName: ${jsonDocs.length} docs")
      }

    case RetrieveFromIndex() =>

    case _ => log.error("Unknown message")
  }

}
