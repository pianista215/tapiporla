package com.tapiporla.microservices.retrievers.indices.ibex35.model

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.retrievers.common.TapiporlaConfig
import com.tapiporla.microservices.retrievers.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.StatGenerated
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ESDAO

object Ibex35Stat {

  def fromMap(map: Map[String,String]) = {
    Ibex35Stat(
      DateTime.parse(map(Ibex35ESDAO.date), DateTimeFormat.forPattern(TapiporlaConfig.globalTimeFormat)),
      map(Ibex35ESDAO.Stats.statsAttr),
      BigDecimal(map(Ibex35ESDAO.Stats.statsValue))
    )
  }

  def fromHit(t: Hit): Ibex35Stat = {
    val map = t.sourceAsMap
    Ibex35Stat(
      DateTime.parse(map(Ibex35ESDAO.date).toString),
      map(Ibex35ESDAO.Stats.statsAttr).toString,
      BigDecimal(map(Ibex35ESDAO.Stats.statsValue).toString)
    )
  }

  def fromStat(stat: StatGenerated): Ibex35Stat =
    Ibex35Stat(stat.date, stat.statType, stat.value)

}

case class Ibex35Stat(
                       date: DateTime,
                       statAttr: String,
                       statValue: BigDecimal
                     ) extends ElasticDocumentInsertable {

  override def json: String = {
    s""" {
       |"${Ibex35ESDAO.date}" : "$date",
       |"${Ibex35ESDAO.Stats.statsAttr}" : "$statAttr",
       |"${Ibex35ESDAO.Stats.statsValue}" : "$statValue"
       |} """.stripMargin
  }

}
