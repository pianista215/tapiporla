package com.tapiporla.microservices.retrievers.common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


case class ScrapyRTRequest(
                            spider_name: String,
                            start_requests: Boolean,
                            request: Map[String, String]
                          )

trait ScrapyRTDefaultProtocol extends SprayJsonSupport with DefaultJsonProtocol{
  implicit val scrapyRTRequest = jsonFormat3(ScrapyRTRequest)
}
