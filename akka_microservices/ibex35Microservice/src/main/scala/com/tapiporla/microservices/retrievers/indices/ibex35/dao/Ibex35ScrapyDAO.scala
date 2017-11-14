package com.tapiporla.microservices.retrievers.indices.ibex35.dao

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import com.tapiporla.microservices.retrievers.common.TapiporlaConfig
import com.tapiporla.microservices.retrievers.common.model.{ScrapyRTDefaultProtocol, ScrapyRTRequest, ScrapyRTResponse}
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ScrapyDAO.{CantRetrieveDataFromIbex35Crawler, IbexDataRetrieved, RetrieveAllIbexData, RetrieveIbexDataFrom}
import com.tapiporla.microservices.retrievers.indices.ibex35.model.Ibex35Historic
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Ibex35ScrapyDAO {

  case object RetrieveAllIbexData
  case class RetrieveIbexDataFrom(date: DateTime)
  case class IbexDataRetrieved(ibexData: Seq[Ibex35Historic])
  case object CantRetrieveDataFromIbex35Crawler
}


/**
  * In charge of retrieve data from the ScrapyRT endpoint
  */
class Ibex35ScrapyDAO extends Actor with ActorLogging with ScrapyRTDefaultProtocol {

  val endpoint = TapiporlaConfig.ScrapyRT.endpoint
  val http = Http(context.system)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive = {

    case RetrieveAllIbexData =>
      retrieveIbexDataFrom(None)

    case RetrieveIbexDataFrom(date) =>
      retrieveIbexDataFrom(Some(date))

    case _ => log.error("Unknown message")
  }

  private def retrieveIbexDataFrom(fromDate: Option[DateTime] = None) =
    Marshal(buildRequest(fromDate)).to[RequestEntity] flatMap { rqEntity =>
      //TODO: Pending from https://github.com/akka/akka-http/issues/1527 (Not applying request settings)
      val timeoutSettings =
        ConnectionPoolSettings(context.system.settings.config).withIdleTimeout(10 minutes)

      http.singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = endpoint,
          entity = rqEntity
        ), settings = timeoutSettings) flatMap { response =>
        Unmarshal(response).to[ScrapyRTResponse] map { scrapyResp =>
          scrapyResp.items.map(Ibex35Historic.fromMap)
        }
      }
    } map { items =>
      IbexDataRetrieved(items)
    } recover {
      case e: Exception =>
        log.error(s"Error retrieving data from Crawler:", e)
        CantRetrieveDataFromIbex35Crawler
    } pipeTo sender

  private def buildRequest(fromDate: Option[DateTime] = None): ScrapyRTRequest =
    fromDate map { date =>
      ScrapyRTRequest("ibex35", true, Map("lookup_until_date" -> date.toString("dd-MM-yyyy")))
    } getOrElse
      ScrapyRTRequest("ibex35", true, Map.empty)

}
