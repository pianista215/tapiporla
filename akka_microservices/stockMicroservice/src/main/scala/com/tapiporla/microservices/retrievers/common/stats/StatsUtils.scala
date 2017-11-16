package com.tapiporla.microservices.retrievers.common.stats

/**
  * Common point for mathematical operations related with Stats
  */
object StatsUtils {

  def mean(data: Seq[BigDecimal]): BigDecimal =
    data.sum / data.length

}
