package com.tapiporla.microservices.retrievers.indices.ibex35

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpRequest, RequestEntity}
import akka.stream.ActorMaterializer
import com.tapiporla.microservices.retrievers.common.{ScrapyRTDefaultProtocol, ScrapyRTRequest}
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35CrawlerAPI.{RetrieveAllIbexData, RetrieveIbexDataFrom}

import scala.concurrent.ExecutionContext.Implicits.global

object Ibex35CrawlerAPI {

  object RetrieveAllIbexData
  case class RetrieveIbexDataFrom(date: Date)
  case class IbexDataRetrieved(ibexData: Seq[Ibex35Historic])

}

trait Ibex35CrawlerJsonSupport extends ScrapyRTDefaultProtocol {
  implicit val historicDataFormat = jsonFormat4(Ibex35Historic)
}

/**
  * In charge of retrieve data from the ScrapyRT endpoint
  */
class Ibex35CrawlerAPI extends Actor with ActorLogging with Ibex35CrawlerJsonSupport{

  //TODO: Settings
  val endpoint = "http://localhost:9081/crawl.json"
  val http = Http(context.system)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive = {

    case RetrieveAllIbexData =>
      Marshal(buildRequest()).to[RequestEntity] flatMap { rqEntity =>
        http.singleRequest(HttpRequest(uri=endpoint, entity = rqEntity))
      }

    case RetrieveIbexDataFrom(date) =>

    case _ => log.error("Unknown message")
  }

  private def buildRequest(fromDate: Option[Date] = None): ScrapyRTRequest = {
    val dateParsed = "23-06-2017" //TODO: From
    ScrapyRTRequest("ibex35", false, Map("lookup_until_date" -> dateParsed))
  }

}
