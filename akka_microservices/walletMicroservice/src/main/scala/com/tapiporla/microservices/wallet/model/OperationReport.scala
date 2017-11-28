package com.tapiporla.microservices.wallet.model

import com.tapiporla.microservices.wallet.common.MathUtils


object OperationReport {

  def getBaseCost(userEquity: UserEquity): BigDecimal =
    MathUtils.normalize(
      userEquity.purchases.map { purchase =>
        purchase.pricePerShare * purchase.quantity
      } sum
    )

  def getTotalFees(userEquity: UserEquity): BigDecimal = //TODO: Sell fee
    MathUtils.normalize(
      userEquity.purchases.map(_.fee).sum +
        userEquity.dividends.map(_.fee).sum +
        userEquity.maintenanceFees.map(_.fee).sum
    )

  def getDividendsProfit(userEquity: UserEquity): BigDecimal =
    MathUtils.normalize(
      userEquity.dividends.map(_.profit).sum
    )


  def fromUserEquity(
                      userEquity: UserEquity,
                      shareSellPrice: BigDecimal
                    ): OperationReport = {

    val baseCost = getBaseCost(userEquity)

    val totalFees = getTotalFees(userEquity)

    val finalCost = MathUtils.normalize(baseCost + totalFees)

    val salePrice = MathUtils.normalize(
      shareSellPrice * userEquity.purchases.map(_.quantity).sum
    )

    val grossProfit = MathUtils.normalize(salePrice - finalCost)

    val dividendsProfit = getDividendsProfit(userEquity)

    val netProfit = MathUtils.normalize(grossProfit + dividendsProfit)

    val percentage = MathUtils.normalize(netProfit / finalCost * 100)

    OperationReport(
      baseCost,
      totalFees,
      finalCost,
      salePrice,
      grossProfit,
      dividendsProfit,
      netProfit,
      percentage
    )
  }

}



case class OperationReport(
                          baseCost: BigDecimal, //Purchases without fee
                          totalFees: BigDecimal, //Sum of all comissions (including dividens or maintenance)
                          finalCost: BigDecimal, //baseCost + totalFees
                          salePrice: BigDecimal,
                          grossProfit: BigDecimal, //salePrice - finalCost
                          dividensProfit: BigDecimal, //Sum of all dividends
                          netProfit: BigDecimal, //grossProfit + dividendsProfit
                          percentage: BigDecimal, //netProfit/finalCost * 100
                          )
