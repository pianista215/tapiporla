package com.tapiporla.microservices.retrievers.indices.ibex35

import com.tapiporla.microservices.retrievers.common.ElasticDAO
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{Hit, Indexable}
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.admin.TypesExistsDefinition

object Ibex35ESDAO {
  val index = "ibex35"
  val typeName = "historic"
  val date = "date"
  val closeValue = "close_value"
  val minValue = "min_value"
  val maxValue = "max_value"
}

class Ibex35ESDAO extends ElasticDAO {

  override def index = Ibex35ESDAO.index

  override def typeName = Ibex35ESDAO.typeName

  override def indexCreation: CreateIndexDefinition =
      createIndex(Ibex35ESDAO.index).mappings(
        mapping(Ibex35ESDAO.typeName) as(
          dateField(Ibex35ESDAO.date),
          doubleField(Ibex35ESDAO.closeValue),
          doubleField(Ibex35ESDAO.minValue),
          doubleField(Ibex35ESDAO.maxValue)
        )
    )

}
