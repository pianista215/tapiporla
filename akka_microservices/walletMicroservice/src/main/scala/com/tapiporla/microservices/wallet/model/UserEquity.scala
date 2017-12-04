package com.tapiporla.microservices.wallet.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tapiporla.microservices.wallet.common.MathUtils

object UserEquity {

  def empty(user: String, equity: String): UserEquity =
    UserEquity(user, equity, 0, 0.0, Seq(), Seq(), Seq())

}

case class UserEquity(
                       user: String,
                       equity: String,
                       numberOfShares: Int,
                       averageSharePrice: BigDecimal,
                       purchases: Seq[Purchase],
                       dividends: Seq[Dividend],
                       maintenanceFees: Seq[MaintenanceFee]
                     ) {

  @JsonIgnore
  def getElasticId: String =
    s"$user@$equity"

  def withPurchase(p: Purchase): UserEquity =
    copy(
      numberOfShares = numberOfShares + p.quantity,
      averageSharePrice = generateAvgPrice(purchases :+ p, dividends, maintenanceFees),
      purchases = (purchases :+ p).sortBy(_.date.toDate)
    )

  def withDividend(d: Dividend): UserEquity =
    copy(
      dividends = (dividends :+ d).sortBy(_.date.toDate),
      averageSharePrice = generateAvgPrice(purchases, dividends :+ d, maintenanceFees),
    )

  def withMaintenanceFee(f: MaintenanceFee): UserEquity =
    copy(
      maintenanceFees = (maintenanceFees :+ f).sortBy(_.date.toDate),
      averageSharePrice = generateAvgPrice(purchases, dividends, maintenanceFees :+ f)
    )

  def generateAvgPrice(purchases: Seq[Purchase], dividends: Seq[Dividend], fees: Seq[MaintenanceFee]): BigDecimal = {
    MathUtils.normalize (
      (

      purchases.map(p => p.quantity * p.pricePerShare + p.fee).sum +
      dividends.map(d => d.fee - d.profit).sum +
      maintenanceFees.map(_.fee).sum

      ) / generateNumberOfShares(purchases)
    )
  }

  def generateNumberOfShares(purchases: Seq[Purchase]): Int =
    purchases.map(_.quantity).sum

}

//TODO: Closed operations