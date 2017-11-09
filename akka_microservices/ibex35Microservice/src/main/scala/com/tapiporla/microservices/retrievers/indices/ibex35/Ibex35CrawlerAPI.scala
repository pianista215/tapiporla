package com.tapiporla.microservices.retrievers.indices.ibex35

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, ResponseEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.tapiporla.microservices.retrievers.common.{ScrapyRTDefaultProtocol, ScrapyRTRequest, ScrapyRTResponse}
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35CrawlerAPI.{CantRetrieveDataFromIbex35Crawler, IbexDataRetrieved, RetrieveAllIbexData, RetrieveIbexDataFrom}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.pipe
import org.joda.time.DateTime

object Ibex35CrawlerAPI {

  object RetrieveAllIbexData
  case class RetrieveIbexDataFrom(date: DateTime)
  case class IbexDataRetrieved(ibexData: Seq[Ibex35Historic])
  object CantRetrieveDataFromIbex35Crawler
}


/**
  * In charge of retrieve data from the ScrapyRT endpoint
  */
class Ibex35CrawlerAPI extends Actor with ActorLogging with ScrapyRTDefaultProtocol {

  //TODO: Settings
  val endpoint = "http://localhost:9080/crawl.json"
  val http = Http(context.system)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive = {

    case RetrieveAllIbexData => //TODO: Errors handling
      retrieveIbexDataFrom(None)

    case RetrieveIbexDataFrom(date) =>
      retrieveIbexDataFrom(Some(date))

    case _ => log.error("Unknown message")
  }

  private def retrieveIbexDataFrom(fromDate: Option[DateTime] = None) =
    Marshal(buildRequest(fromDate)).to[RequestEntity] flatMap { rqEntity =>
      http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = endpoint, entity = rqEntity)) flatMap { response =>
        Unmarshal(response).to[ScrapyRTResponse] map { scrapyResp =>
          scrapyResp.items.map(Ibex35Historic.fromMap)
        }
      }
    } map { items =>
      IbexDataRetrieved(items)
    } recover {
      case e: Exception =>
        log.error(s"Error retrieving data from Crawler: $e.")
        CantRetrieveDataFromIbex35Crawler
    } pipeTo (sender)

  private def buildRequest(fromDate: Option[DateTime] = None): ScrapyRTRequest = {
    fromDate map { date =>
      ScrapyRTRequest("ibex35", true, Map("lookup_until_date" -> date.toString("dd-MM-yyyy")))
    } getOrElse
      ScrapyRTRequest("ibex35", true, Map.empty)
  }

}
