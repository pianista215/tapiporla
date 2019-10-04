package com.tapiporla.microservices.retrievers.common.stats

/**
  * Common point for mathematical operations related with Stats
  */
object StatsUtils {

  def mean(data: Seq[BigDecimal]): BigDecimal =
    data.sum / data.length

  def ema(currentDay: BigDecimal, previousEma: BigDecimal, n: Int): BigDecimal = {
    val k = 2 / (n.toDouble + 1)
    currentDay * k + previousEma * (1-k)
  }

}
