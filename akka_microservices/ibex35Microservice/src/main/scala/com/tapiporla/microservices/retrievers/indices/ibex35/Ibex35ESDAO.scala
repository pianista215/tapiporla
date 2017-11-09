package com.tapiporla.microservices.retrievers.indices.ibex35

import com.tapiporla.microservices.retrievers.common.ElasticDAO
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Indexable
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition

object Ibex35ESDAO {
  val index = "ibex35"
  val mapping = "historic"
  val date = "date"
  val closeValue = "close_value"
  val minValue = "min_value"
  val maxValue = "max_value"

  def json(t: Ibex35Historic): String = s""" { "$date" : "${t.date}", "$closeValue" : "${t.closeValue}", "$minValue" : "${t.minValue}", "$maxValue" : "${t.maxValue}"  } """
}

class Ibex35ESDAO extends ElasticDAO {

  def indices: Seq[CreateIndexDefinition] = {
    Seq(
      createIndex("ibex35").mappings(
        mapping("historic") as(
          dateField(Ibex35ESDAO.date),
          doubleField(Ibex35ESDAO.closeValue),
          doubleField(Ibex35ESDAO.minValue),
          doubleField(Ibex35ESDAO.maxValue)
        )
      )
    )
  }

}
