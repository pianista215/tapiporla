package com.tapiporla.microservices.retrievers.indices.ibex35

import com.github.nscala_time.time.Imports._

object Ibex35Historic {
  def fromMap(map: Map[String,String]) = {
    Ibex35Historic(
      DateTime.parse(map("date"), DateTimeFormat.forPattern("dd-MM-yyyy")),
      BigDecimal(map("close_value")),
      BigDecimal(map("min_value")),
      BigDecimal(map("max_value"))
    )
  }
}

case class Ibex35Historic(
                           date: DateTime,
                           closeValue: BigDecimal,
                           minValue: BigDecimal,
                           maxValue: BigDecimal
                         )
