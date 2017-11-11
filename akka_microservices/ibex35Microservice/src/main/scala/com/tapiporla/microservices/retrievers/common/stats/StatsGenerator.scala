package com.tapiporla.microservices.retrievers.common.stats

import org.joda.time.DateTime

import scala.annotation.tailrec

/**
  * Grand master of the Stats generation
  * Supports:
  * MM200
  * MM40
  * MM10
  */
object StatsGenerator {

  type MMDefition = (String, Int)

  val MM200 = ("MM200", 200)
  val MM100 = ("MM100", 100)
  val MM40 = ("MM40", 40)
  val MM20 = ("MM20", 20)

  //If not enough elements are provided we work with the provided elements
  val START_ELEMENTS_RECOMMENDED: Int = MM200._2 * 2

  type Stat = (DateTime, String, BigDecimal)
  type Data = (DateTime, BigDecimal)

  def generateStatsFor(data: Seq[Data]): Seq[Stat] =
    generateMM200From(data) ++
      generateMM100From(data) ++
      generateMM40From(data) ++
      generateMM20From(data)

  def generateMM200From(data: Seq[Data]): Seq[Stat] =
    generateMM(data, MM200._2) map {item => (item._1, MM200._1, item._2)}

  def generateMM100From(data: Seq[Data]): Seq[Stat] =
    generateMM(data, MM100._2) map {item => (item._1, MM100._1, item._2)}

  def generateMM40From(data: Seq[Data]): Seq[Stat] =
    generateMM(data, MM40._2) map {item => (item._1, MM40._1, item._2)}

  def generateMM20From(data: Seq[Data]): Seq[Stat] =
    generateMM(data, MM20._2) map {item => (item._1, MM20._1, item._2)}

  def generateMM(initial: Seq[Data], number: Int): Seq[Data] = {

    @tailrec
    def helper(pending: Seq[Data], passed: Seq[Data], accum: Seq[Data]): Seq[Data] = {
      if(pending.isEmpty)
        accum
      else {
        val newChunk =
          if(passed.length < number) //(number == initial length elements)
             passed :+ pending.head
          else
            (passed drop 1) :+ pending.head
        helper(
          pending.tail,
          newChunk,
          accum :+ (pending.head._1, mean(newChunk map (_._2)))
        )
      }

    }

    val sortedByDate = initial.sortBy(_._1.toDate)
    helper(sortedByDate drop number - 1, sortedByDate take number - 1, Seq())
  }

  def mean(data: Seq[BigDecimal]): BigDecimal =
    data.sum / data.length

}
