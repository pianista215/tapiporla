package com.tapiporla.microservices.retrievers.indices.stock.model

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.retrievers.common.TapiporlaConfig
import com.tapiporla.microservices.retrievers.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.StatGenerated
import com.tapiporla.microservices.retrievers.indices.stock.dao.StockESDAO

object StockStat {

  def fromMap(values: Map[String,String]) = {
    StockStat(
      DateTime.parse(values(StockESDAO.date), DateTimeFormat.forPattern(TapiporlaConfig.globalTimeFormat)),
      values(StockESDAO.Stats.statsAttr),
      BigDecimal(values(StockESDAO.Stats.statsValue))
    )
  }

  def fromHit(t: Hit): StockStat = {
    val map = t.sourceAsMap
    StockStat(
      DateTime.parse(map(StockESDAO.date).toString),
      map(StockESDAO.Stats.statsAttr).toString,
      BigDecimal(map(StockESDAO.Stats.statsValue).toString)
    )
  }

  def fromStat(stat: StatGenerated): StockStat =
    StockStat(stat.date, stat.statType, stat.value)

}

case class StockStat(
                       date: DateTime,
                       statAttr: String,
                       statValue: BigDecimal
                     ) extends ElasticDocumentInsertable {

  override def json: String = {
    s""" {
       |"${StockESDAO.date}" : "$date",
       |"${StockESDAO.Stats.statsAttr}" : "$statAttr",
       |"${StockESDAO.Stats.statsValue}" : "$statValue"
       |} """.stripMargin
  }

}
