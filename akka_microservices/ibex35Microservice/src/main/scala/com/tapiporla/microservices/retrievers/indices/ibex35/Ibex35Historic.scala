package com.tapiporla.microservices.retrievers.indices.ibex35

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.retrievers.indices.ibex35.Ibex35ESDAO.{closeValue, date, maxValue, minValue}

object Ibex35Historic {
  def fromMap(map: Map[String,String]) = {
    Ibex35Historic(
      DateTime.parse(map("date"), DateTimeFormat.forPattern("dd-MM-yyyy")),
      BigDecimal(map("close_value")),
      BigDecimal(map("min_value")),
      BigDecimal(map("max_value"))
    )
  }

  def json(t: Ibex35Historic): String = {
    s""" { "$date" : "${t.date}", "$closeValue" : "${t.closeValue}", "$minValue" : "${t.minValue}", "$maxValue" : "${t.maxValue}"  } """
  }

  def fromHit(t: Hit): Ibex35Historic = {
    val map = t.sourceAsMap
    Ibex35Historic(
      DateTime.parse(map(date).toString),
      BigDecimal(map(closeValue).toString),
      BigDecimal(map(minValue).toString),
      BigDecimal(map(maxValue).toString)
    )
  }
}

case class Ibex35Historic(
                           date: DateTime,
                           closeValue: BigDecimal,
                           minValue: BigDecimal,
                           maxValue: BigDecimal
                         )
