package com.tapiporla.microservices.retrievers.common.model

import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.Data

trait DataInputExtractable {

  def toStatInputData: Data
}
