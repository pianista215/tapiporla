package com.tapiporla.microservices.retrievers.indices.stock.dao

import akka.actor.Props
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.tapiporla.microservices.retrievers.common.{ElasticDAO, TapiporlaConfig}

object StockESDAO {
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

  def props(esIndex: String): Props = Props(new StockESDAO(esIndex))

}

class StockESDAO(esIndex: String) extends ElasticDAO {

  override def indexCreation: CreateIndexDefinition =
      createIndex(esIndex).mappings(

        mapping(StockESDAO.Historic.typeName) as(
          dateField(StockESDAO.date),
          doubleField(StockESDAO.Historic.openingValue),
          doubleField(StockESDAO.Historic.closeValue),
          doubleField(StockESDAO.Historic.minValue),
          doubleField(StockESDAO.Historic.maxValue),
          doubleField(StockESDAO.Historic.volume)
        ),

        mapping(StockESDAO.Stats.typeName) as (
          dateField(StockESDAO.date),
          keywordField(StockESDAO.Stats.statsAttr),
          doubleField(StockESDAO.Stats.statsValue)
        )

    )

}
