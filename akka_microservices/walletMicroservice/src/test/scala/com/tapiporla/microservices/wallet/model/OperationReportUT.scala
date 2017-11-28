package com.tapiporla.microservices.wallet.model

import com.tapiporla.microservices.wallet.common.MathUtils
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

class OperationReportUT extends FlatSpec with Matchers {

  val baseExample = UserEquity(
    "example",
    "exampleEquity",
    300,
    6.00,
    Seq(
      Purchase(DateTime.now(), 150, 5.00, 5.00),
      Purchase(DateTime.now(), 150, 7.00, 5.00),
    ),
    Seq(
      Dividend(DateTime.now(), 10.75, 2.00),
      Dividend(DateTime.now(), 15.00, 1.00)
    ),
    Seq(
      MaintenanceFee(DateTime.now(), 1.00),
      MaintenanceFee(DateTime.now(), 1.50),
    )
  )

  "An OperationReport" should "be able to compute the baseCost" in {
    OperationReport.getBaseCost(baseExample) should be (BigDecimal(1800.00))
  }

  it should "be able to compute the total fees" in {
    OperationReport.getTotalFees(baseExample) should be (BigDecimal(15.50))
  }

  it should "be able to compute the total profit of the dividends" in {
    OperationReport.getDividendsProfit(baseExample) should be (BigDecimal(25.75))
  }

  it should "comput the full report for a given user equity information" in {
    OperationReport.fromUserEquity(baseExample, 8.00) should be (
     OperationReport(
       MathUtils.normalize(1800.00),
       MathUtils.normalize(15.50),
       MathUtils.normalize(1815.50),
       MathUtils.normalize(2400.00),
       MathUtils.normalize(584.50),
       MathUtils.normalize(25.75),
       MathUtils.normalize(610.25),
       MathUtils.normalize(33.61)
     )
    )
  }
}
