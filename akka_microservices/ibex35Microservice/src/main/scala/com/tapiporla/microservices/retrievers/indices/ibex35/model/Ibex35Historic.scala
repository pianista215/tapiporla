package com.tapiporla.microservices.retrievers.indices.ibex35.model

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ESDAO

object Ibex35Historic {
  def fromMap(map: Map[String,String]) = {
    Ibex35Historic(
      DateTime.parse(map(Ibex35ESDAO.date), DateTimeFormat.forPattern("dd-MM-yyyy")),
      BigDecimal(map(Ibex35ESDAO.Historic.closeValue)),
      BigDecimal(map(Ibex35ESDAO.Historic.minValue)),
      BigDecimal(map(Ibex35ESDAO.Historic.maxValue))
    )
  }

  def json(t: Ibex35Historic): String = {
    s""" {
       |"${Ibex35ESDAO.date}" : "${t.date}",
       |"${Ibex35ESDAO.Historic.closeValue}" : "${t.closeValue}",
       |"${Ibex35ESDAO.Historic.minValue}" : "${t.minValue}",
       |"${Ibex35ESDAO.Historic.maxValue}" : "${t.maxValue}"
       |} """.stripMargin
  }

  def fromHit(t: Hit): Ibex35Historic = {
    val map = t.sourceAsMap
    Ibex35Historic(
      DateTime.parse(map(Ibex35ESDAO.date).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.closeValue).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.minValue).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.maxValue).toString)
    )
  }
}

case class Ibex35Historic(
                           date: DateTime,
                           closeValue: BigDecimal,
                           minValue: BigDecimal,
                           maxValue: BigDecimal
                         )
