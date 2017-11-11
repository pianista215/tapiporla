package com.tapiporla.microservices.retrievers.indices.ibex35.model

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ESDAO

object Ibex35Stat {
  def fromMap(map: Map[String,String]) = {
    Ibex35Stat(
      DateTime.parse(map(Ibex35ESDAO.date), DateTimeFormat.forPattern("dd-MM-yyyy")),
      map(Ibex35ESDAO.Stats.statsAttr),
      BigDecimal(map(Ibex35ESDAO.Stats.statsValue))
    )
  }

  def json(t: Ibex35Stat): String = {
    s""" {
       |"${Ibex35ESDAO.date}" : "${t.date}",
       |"${Ibex35ESDAO.Stats.statsAttr}" : "${t.statAttr}",
       |"${Ibex35ESDAO.Stats.statsValue}" : "${t.statValue}"
       |} """.stripMargin
  }

  def fromHit(t: Hit): Ibex35Stat = {
    val map = t.sourceAsMap
    Ibex35Stat(
      DateTime.parse(map(Ibex35ESDAO.date).toString),
      map(Ibex35ESDAO.Stats.statsAttr).toString,
      BigDecimal(map(Ibex35ESDAO.Stats.statsValue).toString)
    )
  }
}

case class Ibex35Stat(
                           date: DateTime,
                           statAttr: String,
                           statValue: BigDecimal
                         )
