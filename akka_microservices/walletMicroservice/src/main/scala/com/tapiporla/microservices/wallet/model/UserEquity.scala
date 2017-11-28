package com.tapiporla.microservices.wallet.model

import org.joda.time.DateTime

case class UserEquity(
                       user: String,
                       equity: String,
                       numberOfShares: Int,
                       averageSharePrice: BigDecimal,
                       purchases: Seq[Purchase],
                       dividends: Seq[Dividend],
                       maintenanceFees: Seq[MaintenanceFee]
                     )


case class Purchase(
                    date: DateTime,
                    quantity: Int,
                    pricePerShare: BigDecimal,
                    fee: BigDecimal
                    )

case class Dividend(
                    date: DateTime,
                    profit: BigDecimal,
                    fee: BigDecimal
                    )

case class MaintenanceFee(
                          date: DateTime,
                          fee: BigDecimal
                          )


//TODO: Closed operations