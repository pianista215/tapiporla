package com.tapiporla.microservices.retrievers.common.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


case class ScrapyRTRequest(
                            spider_name: String,
                            start_requests: Boolean,
                            parameters: Map[String,String]
                          )

case class ScrapyRTResponse(
                           status: String,
                           spider_name: String,
                           items: Seq[Map[String,String]]
                           )

trait ScrapyRTDefaultProtocol extends SprayJsonSupport with DefaultJsonProtocol{
  implicit val scrapyRTRequest = jsonFormat3(ScrapyRTRequest)
  implicit val scrapyRTResponse = jsonFormat3(ScrapyRTResponse)
}
