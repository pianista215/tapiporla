package com.tapiporla.microservices.common.stats

import com.github.nscala_time.time.Imports.DateTimeFormat
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator
import com.tapiporla.microservices.retrievers.common.stats.StatsGenerator.{Data, Stat}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

class StatsGeneratorUT extends FlatSpec with Matchers{

  def generateDates(count: Int): DateTime =
    DateTime.now().plusDays(count)

  "An StatsGenerator" should "be able to create a mean" in {

    val example: Seq[BigDecimal] = (0 to 10) map {x => BigDecimal(x)}
    StatsGenerator.mean(example) should be (BigDecimal(5))

    val example2: Seq[BigDecimal] = (0 to 9) map {x => BigDecimal(x)}
    StatsGenerator.mean(example2) should be (BigDecimal(4.5))

  }

  it should "return empty stats when no enough elements provided" in {

    val example: Seq[Data] = (0 to 18) map { x =>
      (generateDates(x), BigDecimal(x))
    }

    example should have length(19)

    StatsGenerator.generateMM(example, 20) should be (Seq())
    StatsGenerator.generateMM20From(example) should be (Seq())
  }

  it should "generate correctly mean stats provided" in {

    val example: Seq[Data] = (1 to 10) map { x =>
      (generateDates(x), BigDecimal(x))
    }

    example should have length(10)

    StatsGenerator.generateMM(example, 10) should be (
      Seq(
        (example.last._1, BigDecimal(5.5))
      )
    )

    val example2: Seq[Data] = (1 to 12) map { x =>
      (generateDates(x), BigDecimal(x*2))
    }

    example2 should have length(12)

    val lastThree = (example2 drop 9)

    StatsGenerator.generateMM(example2, 10) should be (
      Seq(
        (lastThree.head._1, BigDecimal(11)),
        (lastThree(1)._1, BigDecimal(13)),
        (lastThree.last._1, BigDecimal(15))
      )
    )
  }

}
