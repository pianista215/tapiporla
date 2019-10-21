package com.tapiporla.microservices.retrievers.common.stats

/**
  * Common point for mathematical operations related with Stats
  */
object StatsUtils {

  def mean(data: Seq[BigDecimal]): BigDecimal =
    data.sum / data.length

  def median(data: Seq[BigDecimal]): BigDecimal = {
    val sortedSeq = data.sortWith(_ < _)
    if (data.size % 2 == 1) sortedSeq(sortedSeq.size / 2)
    else {
      val (up, down) = sortedSeq.splitAt(data.size / 2)
      (up.last + down.head) / 2
    }
  }

  def ema(currentDay: BigDecimal, previousEma: BigDecimal, n: Int): BigDecimal = {
    val k = 2 / (n.toDouble + 1)
    currentDay * k + previousEma * (1-k)
  }

  def macd(ema1: BigDecimal, ema2: BigDecimal): BigDecimal = {
    ema1 - ema2
  }

  def macdSignal(macd: Seq[BigDecimal], numberOfPeriods: Int = 9): BigDecimal = {
    val previousEma = mean(macd.take(numberOfPeriods))
    ema(macd(numberOfPeriods + 1), previousEma, numberOfPeriods)
  }
  
  def calculateStochK(currentDay: BigDecimal, history: Seq[BigDecimal]): BigDecimal = {
    val min = history.reduceLeft(_ min _)
    val max = history.reduceLeft(_ max _)
    (currentDay - min) / (max - min)*100
  }

  def calculateStochD(stoch: Seq[BigDecimal], numberOfPeriods: Int = 9): BigDecimal = {
    mean(stoch.take(numberOfPeriods))
  }

}
