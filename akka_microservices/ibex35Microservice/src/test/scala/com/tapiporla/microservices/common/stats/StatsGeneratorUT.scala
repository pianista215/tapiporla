package com.tapiporla.microservices.common.stats

import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.{MMDefinition, StatDataInput, StatGenerated}
import com.tapiporla.microservices.retrievers.common.stats.{StatsGenerator, StatsUtils}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

class StatsGeneratorUT extends FlatSpec with Matchers {

  def generateDates(count: Int): DateTime =
    DateTime.now().plusDays(count)

  val MM20 = MMDefinition.from(20)
  val MM10 = MMDefinition.from(10)
  val MM5 = MMDefinition.from(5)

  "An StatsGenerator" should "be able to create a mean" in {

    val example: Seq[BigDecimal] = (0 to 10) map {x => BigDecimal(x)}
    StatsUtils.mean(example) should be (BigDecimal(5))

    val example2: Seq[BigDecimal] = (0 to 9) map {x => BigDecimal(x)}
    StatsUtils.mean(example2) should be (BigDecimal(4.5))

  }

  it should "return empty stats when no enough elements provided" in {

    val example: Seq[StatDataInput] = (0 to 18) map { x =>
      StatDataInput(generateDates(x), BigDecimal(x))
    }

    example should have length(19)

    StatsGenerator.generateMultipleMMs(example, Seq(), Seq(MM20)) should be (Seq())
  }

  it should "generate correctly mean stats provided" in {

    val example: Seq[StatDataInput] = (1 to 10) map { x =>
      StatDataInput(generateDates(x), BigDecimal(x))
    }

    example should have length(10)

    StatsGenerator.generateMultipleMMs(example, Seq(), Seq(MM10)) should be (
      Seq(
        StatGenerated(MM10.identifier, example.last.date, BigDecimal(5.5))
      )
    )

    val example2: Seq[StatDataInput] = (1 to 12) map { x =>
      StatDataInput(generateDates(x), BigDecimal(x*2))
    }

    example2 should have length(12)

    val lastThree = example2 drop 9

    StatsGenerator.generateMultipleMMs(example2, Seq(), Seq(MM10)) should be (
      Seq(
        StatGenerated(MM10.identifier, lastThree.head.date, BigDecimal(11)),
        StatGenerated(MM10.identifier, lastThree(1).date, BigDecimal(13)),
        StatGenerated(MM10.identifier, lastThree.last.date, BigDecimal(15))
      )
    )

    val lastEight = example2 drop 4

    StatsGenerator.generateMultipleMMs(example2, Seq(), Seq(MM5)) should be (
      Seq(
        StatGenerated(MM5.identifier, lastEight.head.date, BigDecimal(6)), //2+4+6+8+10
        StatGenerated(MM5.identifier, lastEight(1).date, BigDecimal(8)),
        StatGenerated(MM5.identifier, lastEight(2).date, BigDecimal(10)),
        StatGenerated(MM5.identifier, lastEight(3).date, BigDecimal(12)),
        StatGenerated(MM5.identifier, lastEight(4).date, BigDecimal(14)),
        StatGenerated(MM5.identifier, lastEight(5).date, BigDecimal(16)),
        StatGenerated(MM5.identifier, lastEight(6).date, BigDecimal(18)),
        StatGenerated(MM5.identifier, lastEight.last.date, BigDecimal(20))
      )
    )

  }

  it should "be able to complete the chunk with historic data and process the mean" in {

    val example: Seq[StatDataInput] = (5 to 10) map { x =>
      StatDataInput(generateDates(x), BigDecimal(x))
    }

    example should have length(6)

    val previous: Seq[StatDataInput] = (1 to 4) map { x =>
      StatDataInput(generateDates(x), BigDecimal(x))
    }

    previous should have length(4)


    StatsGenerator.generateMultipleMMs(example, previous, Seq(MM10)) should be (
      Seq(
        StatGenerated(MM10.identifier, example.last.date, BigDecimal(5.5))
      )
    )

    val example2: Seq[StatDataInput] = (5 to 12) map { x =>
      StatDataInput(generateDates(x), BigDecimal(x*2))
    }

    example2 should have length(8)

    val previous2: Seq[StatDataInput] = (1 to 4) map { x =>
      StatDataInput(generateDates(x), BigDecimal(x*2))
    }

    previous2 should have length(4)

    val lastThree = example2 drop 5

    StatsGenerator.generateMultipleMMs(example2, previous2, Seq(MM10)) should be (
      Seq(
        StatGenerated(MM10.identifier, lastThree.head.date, BigDecimal(11)),
        StatGenerated(MM10.identifier, lastThree(1).date, BigDecimal(13)),
        StatGenerated(MM10.identifier, lastThree.last.date, BigDecimal(15))
      )
    )

    val lastEight = example2

    StatsGenerator.generateMultipleMMs(example2, previous2, Seq(MM5)) should be (
      Seq(
        StatGenerated(MM5.identifier, lastEight.head.date, BigDecimal(6)), //2+4+6+8+10
        StatGenerated(MM5.identifier, lastEight(1).date, BigDecimal(8)),
        StatGenerated(MM5.identifier, lastEight(2).date, BigDecimal(10)),
        StatGenerated(MM5.identifier, lastEight(3).date, BigDecimal(12)),
        StatGenerated(MM5.identifier, lastEight(4).date, BigDecimal(14)),
        StatGenerated(MM5.identifier, lastEight(5).date, BigDecimal(16)),
        StatGenerated(MM5.identifier, lastEight(6).date, BigDecimal(18)),
        StatGenerated(MM5.identifier, lastEight.last.date, BigDecimal(20))
      )
    )

  }

  //TODO: need more testing here (for example overflowing the previous... etc)

}
