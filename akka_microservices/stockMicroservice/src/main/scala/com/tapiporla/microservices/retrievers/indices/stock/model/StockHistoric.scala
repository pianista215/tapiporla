package com.tapiporla.microservices.retrievers.indices.stock.model

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.retrievers.common.TapiporlaConfig
import com.tapiporla.microservices.retrievers.common.model.{DataInputExtractable, ElasticDocumentInsertable}
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.StatDataInput
import com.tapiporla.microservices.retrievers.indices.stock.dao.StockESDAO

object StockHistoric {
  def fromMap(values: Map[String,String]) = {
    StockHistoric(
      DateTime.parse(values(StockESDAO.date), DateTimeFormat.forPattern(TapiporlaConfig.globalTimeFormat)),
      BigDecimal(values(StockESDAO.Historic.openingValue)),
      BigDecimal(values(StockESDAO.Historic.closeValue)),
      BigDecimal(values(StockESDAO.Historic.minValue)),
      BigDecimal(values(StockESDAO.Historic.maxValue)),
      BigDecimal(values(StockESDAO.Historic.volume))
    )
  }

  def fromHit(t: Hit): StockHistoric = {
    val map = t.sourceAsMap
    StockHistoric(
      DateTime.parse(map(StockESDAO.date).toString),
      BigDecimal(map(StockESDAO.Historic.openingValue).toString),
      BigDecimal(map(StockESDAO.Historic.closeValue).toString),
      BigDecimal(map(StockESDAO.Historic.minValue).toString),
      BigDecimal(map(StockESDAO.Historic.maxValue).toString),
      BigDecimal(map(StockESDAO.Historic.volume).toString)
    )
  }
}

case class StockHistoric(
                           date: DateTime,
                           openingValue: BigDecimal,
                           closeValue: BigDecimal,
                           minValue: BigDecimal,
                           maxValue: BigDecimal,
                           volume: BigDecimal
                         ) extends ElasticDocumentInsertable with DataInputExtractable {

  override def json: String = { //TODO: Check if automatic json generation could be done (Marshallers)
    s""" {
       |"${StockESDAO.date}" : "$date",
       |"${StockESDAO.Historic.openingValue}" : "$openingValue",
       |"${StockESDAO.Historic.closeValue}" : "$closeValue",
       |"${StockESDAO.Historic.minValue}" : "$minValue",
       |"${StockESDAO.Historic.maxValue}" : "$maxValue",
       |"${StockESDAO.Historic.volume}" : "$volume"
       |} """.stripMargin
  }

  override def toStatInputData: StatDataInput =
    StatDataInput(date, closeValue)

}
