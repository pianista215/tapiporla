package com.tapiporla.microservices.retrievers.indices.stock.dao

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import com.tapiporla.microservices.retrievers.common.{TapiporlaActor, TapiporlaConfig}
import com.tapiporla.microservices.retrievers.common.model.{ScrapyRTDefaultProtocol, ScrapyRTParameters, ScrapyRTRequest, ScrapyRTResponse}
import com.tapiporla.microservices.retrievers.indices.stock.dao.StockScrapyDAO.{CantRetrieveDataFromCrawler, RetrieveAllStockData, RetrieveStockDataFrom, StockDataRetrieved}
import com.tapiporla.microservices.retrievers.indices.stock.model.StockHistoric
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object StockScrapyDAO {

  case object RetrieveAllStockData
  case class RetrieveStockDataFrom(date: DateTime)
  case class StockDataRetrieved(stockData: Seq[StockHistoric])
  case object CantRetrieveDataFromCrawler

  def props(): Props = Props(new StockScrapyDAO())
}


/**
  * In charge of retrieve data from the ScrapyRT endpoint
  */
class StockScrapyDAO extends TapiporlaActor with ScrapyRTDefaultProtocol {

  val endpoint = TapiporlaConfig.ScrapyRT.endpoint
  val http = Http(context.system)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive = {

    case RetrieveAllStockData =>
      retrieveStockDataFrom(None)

    case RetrieveStockDataFrom(date) =>
      retrieveStockDataFrom(Some(date))

  }

  private def retrieveStockDataFrom(fromDate: Option[DateTime] = None) =
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
          scrapyResp.items.map(StockHistoric.fromMap)
        }
      }
    } map { items =>
      StockDataRetrieved(items)
    } recover {
      case e: Exception =>
        log.error(s"Error retrieving data from Crawler:", e)
        CantRetrieveDataFromCrawler
    } pipeTo sender

  val crawlerId = TapiporlaConfig.Stock.scrapyCrawler
  val crawlerPath = TapiporlaConfig.Stock.crawlerPath

  private def buildRequest(fromDate: Option[DateTime] = None): ScrapyRTRequest =
    ScrapyRTRequest(crawlerId, start_requests = true, buildParameters(fromDate))

  private def buildParameters(fromDate: Option[DateTime] =  None): ScrapyRTParameters =
    fromDate map { date =>
      ScrapyRTParameters(crawlerPath, Some(date.toString(TapiporlaConfig.globalTimeFormat)))
    } getOrElse
      ScrapyRTParameters(crawlerPath, None)

}
