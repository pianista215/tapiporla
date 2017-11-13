package com.tapiporla.microservices.retrievers.indices.ibex35.model

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.retrievers.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.retrievers.indices.ibex35.dao.Ibex35ESDAO

object Ibex35Historic {
  def fromMap(map: Map[String,String]) = {
    Ibex35Historic(
      DateTime.parse(map(Ibex35ESDAO.date), DateTimeFormat.forPattern("dd-MM-yyyy")),
      BigDecimal(map(Ibex35ESDAO.Historic.openingValue)),
      BigDecimal(map(Ibex35ESDAO.Historic.closeValue)),
      BigDecimal(map(Ibex35ESDAO.Historic.minValue)),
      BigDecimal(map(Ibex35ESDAO.Historic.maxValue)),
      BigDecimal(map(Ibex35ESDAO.Historic.volume))
    )
  }

  def fromHit(t: Hit): Ibex35Historic = {
    val map = t.sourceAsMap
    Ibex35Historic(
      DateTime.parse(map(Ibex35ESDAO.date).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.openingValue).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.closeValue).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.minValue).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.maxValue).toString),
      BigDecimal(map(Ibex35ESDAO.Historic.volume).toString)
    )
  }
}

case class Ibex35Historic(
                           date: DateTime,
                           openingValue: BigDecimal,
                           closeValue: BigDecimal,
                           minValue: BigDecimal,
                           maxValue: BigDecimal,
                           volume: BigDecimal
                         ) extends ElasticDocumentInsertable {

  override def json: String = {
    s""" {
       |"${Ibex35ESDAO.date}" : "$date",
       |"${Ibex35ESDAO.Historic.openingValue}" : "$openingValue",
       |"${Ibex35ESDAO.Historic.closeValue}" : "$closeValue",
       |"${Ibex35ESDAO.Historic.minValue}" : "$minValue",
       |"${Ibex35ESDAO.Historic.maxValue}" : "$maxValue",
       |"${Ibex35ESDAO.Historic.volume}" : "$volume"
       |} """.stripMargin
  }

}
