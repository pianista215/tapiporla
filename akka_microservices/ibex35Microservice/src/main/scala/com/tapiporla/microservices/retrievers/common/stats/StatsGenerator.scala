package com.tapiporla.microservices.retrievers.common.stats

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.annotation.tailrec

/**
  * Grand master of the Stats generation
  * Supports:
  * MM200
  * MM40
  * MM10
  */
object StatsGenerator extends LazyLogging {

  object MMDefinition {

    def from(numberOfItems: Int): MMDefinition =
      MMDefinition(s"MM$numberOfItems", numberOfItems)

  }

  case class MMDefinition(identifier: String, numberOfItems: Int)

  val MM200 = MMDefinition.from(200)
  val MM100 = MMDefinition.from(100)
  val MM40 = MMDefinition.from(40)
  val MM20 = MMDefinition.from(20)

  //If not enough elements are provided we work with the provided elements
  val START_ELEMENTS_RECOMMENDED: Int = MM200.numberOfItems * 2

  //TODO: Create traits to inherit????
  type Stat = (DateTime, String, BigDecimal)
  type Data = (DateTime, BigDecimal)

  /**
    * It will generate Stats using the previous data if needed
    * @param data
    * @param previousData
    * @return
    */
  def generateStatsFor(data: Seq[Data], previousData: Seq[Data]): Seq[Stat] = {
    val dataByDate = data.sortBy(_._1.toDate)
    val previousByDate = data.sortBy(_._1.toDate)

    generateMM200From(dataByDate, previousByDate takeRight MM200.numberOfItems - 1) ++
      generateMM100From(dataByDate, previousByDate takeRight MM100.numberOfItems -1) ++
      generateMM40From(dataByDate, previousByDate takeRight MM40.numberOfItems - 1) ++
      generateMM20From(dataByDate, previousByDate takeRight MM20.numberOfItems -1)
  }

  def generateMM200From(data: Seq[Data], previousData: Seq[Data]): Seq[Stat] =
    generateMM(data, previousData, MM200.numberOfItems) map {item => (item._1, MM200.identifier, item._2)}

  def generateMM100From(data: Seq[Data], previousData: Seq[Data]): Seq[Stat] =
    generateMM(data, previousData, MM100.numberOfItems) map {item => (item._1, MM100.identifier, item._2)}

  def generateMM40From(data: Seq[Data], previousData: Seq[Data]): Seq[Stat] =
    generateMM(data, previousData, MM40.numberOfItems) map {item => (item._1, MM40.identifier, item._2)}

  def generateMM20From(data: Seq[Data], previousData: Seq[Data]): Seq[Stat] =
    generateMM(data, previousData, MM20.numberOfItems) map {item => (item._1, MM20.identifier, item._2)}

  def generateMM(initial: Seq[Data], previousData: Seq[Data], number: Int): Seq[Data] = {

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
          accum :+ (pending.head._1, StatsUtils.mean(newChunk map (_._2)))
        )
      }

    }

    //Complete first chunk (Without enough data, to compute the means, it should be > 0 only in the first iteration of the APP)
    val remainingToCompleteChunk = number - 1 - previousData.length
    if(remainingToCompleteChunk > 0)
      logger.warn("Warning, there are not enough previous data to compute the mean (If you are seeing that in the first iteration of the stats. Ignore it)")
    helper(initial drop remainingToCompleteChunk, previousData ++ (initial take remainingToCompleteChunk), Seq())
  }


}
