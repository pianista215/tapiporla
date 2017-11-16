package com.tapiporla.microservices.retrievers.common.model

import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.StatDataInput

trait DataInputExtractable {

  def toStatInputData: StatDataInput
}
