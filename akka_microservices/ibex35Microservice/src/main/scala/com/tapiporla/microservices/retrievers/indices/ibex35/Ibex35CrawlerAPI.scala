package com.tapiporla.microservices.retrievers.indices.ibex35

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, ResponseEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.tapiporla.microservices.retrievers.common.{ScrapyRTDefaultProtocol, ScrapyRTRequest, ScrapyRTResponse}
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35CrawlerAPI.{RetrieveAllIbexData, RetrieveIbexDataFrom}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Ibex35CrawlerAPI {

  object RetrieveAllIbexData
  case class RetrieveIbexDataFrom(date: Date)
  case class IbexDataRetrieved(ibexData: Seq[Ibex35Historic])

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
      val pp = Marshal(buildRequest()).to[RequestEntity] flatMap { rqEntity =>
        http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = endpoint, entity = rqEntity)) flatMap { response =>
          print(s"RESPONSEEEEE ${response.status}")
          Unmarshal(response).to[ScrapyRTResponse] map { scrapyResp =>
            scrapyResp.items.map { item =>
              Ibex35Historic.fromMap(item)
            } foreach (print)
          }
        }
      }

    case RetrieveIbexDataFrom(date) =>

    case _ => log.error("Unknown message")
  }

  private def buildRequest(fromDate: Option[Date] = None): ScrapyRTRequest = {
    val dateParsed = "23-10-2017" //TODO: From
    ScrapyRTRequest("ibex35", true, dateParsed) //Generic way to pass arguments (Modify docker to allow params map??)
  }

}
