package com.tapiporla.microservices.retrievers.indices.ibex35.dao

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.tapiporla.microservices.retrievers.common.ElasticDAO

object Ibex35ESDAO {
  val index = "ibex35"
  val date = "date"

  object Historic {
    val typeName = "historic"
    val openingValue = "opening_value"
    val closeValue = "close_value"
    val minValue = "min_value"
    val maxValue = "max_value"
    val volume = "volume"
  }

  object Stats {
    val typeName = "stats"
    val statsAttr = "stats_attribute"
    val statsValue = "stats_value"
  }

}

class Ibex35ESDAO extends ElasticDAO {

  override def indexCreation: CreateIndexDefinition =
      createIndex(Ibex35ESDAO.index).mappings(

        mapping(Ibex35ESDAO.Historic.typeName) as(
          dateField(Ibex35ESDAO.date),
          doubleField(Ibex35ESDAO.Historic.openingValue),
          doubleField(Ibex35ESDAO.Historic.closeValue),
          doubleField(Ibex35ESDAO.Historic.minValue),
          doubleField(Ibex35ESDAO.Historic.maxValue),
          doubleField(Ibex35ESDAO.Historic.volume)
        ),

        mapping(Ibex35ESDAO.Stats.typeName) as (
          dateField(Ibex35ESDAO.date),
          keywordField(Ibex35ESDAO.Stats.statsAttr),
          doubleField(Ibex35ESDAO.Stats.statsValue)
        )

    )

}
