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

  case class StatGenerated(statType: String, date: DateTime, value: BigDecimal)
  case class StatDataInput(date: DateTime, value: BigDecimal)


  def generateMultipleMMs(
                           initial: Seq[StatDataInput],
                           previousData: Seq[StatDataInput],
                           mmsToProcess: Seq[MMDefinition]
                         ) = {


    val dataByDate = initial.sortBy(_.date.toDate)
    val previousByDate = previousData.sortBy(_.date.toDate)

    mmsToProcess flatMap { mm =>
      generateMM(dataByDate, previousByDate takeRight mm.numberOfItems - 1, mm)
    }
  }


  protected def generateMM(
                  initial: Seq[StatDataInput],
                  previousData: Seq[StatDataInput],
                  mm: MMDefinition
                ): Seq[StatGenerated] = {

    @tailrec
    def helper(pending: Seq[StatDataInput], passed: Seq[StatDataInput], accum: Seq[StatGenerated]): Seq[StatGenerated] = {
      if (pending.isEmpty)
        accum
      else {
        val newChunk =
          if (passed.length < mm.numberOfItems) //(number == initial length elements)
            passed :+ pending.head
          else
            (passed drop 1) :+ pending.head
        helper(
          pending.tail,
          newChunk,
          accum :+ StatGenerated(mm.identifier, pending.head.date, StatsUtils.mean(newChunk map (_.value)))
        )
      }

    }

    //Complete first chunk (Without enough data, to compute the means, it should be > 0 only in the first iteration of the APP)
    val remainingToCompleteChunk = mm.numberOfItems - 1 - previousData.length
    if (remainingToCompleteChunk > 0)
      logger.warn("Warning, there are not enough previous data to compute the mean (If you are seeing that in the first iteration of the stats. Ignore it)")


    helper(initial drop remainingToCompleteChunk, previousData ++ (initial take remainingToCompleteChunk), Seq())
  }


}
