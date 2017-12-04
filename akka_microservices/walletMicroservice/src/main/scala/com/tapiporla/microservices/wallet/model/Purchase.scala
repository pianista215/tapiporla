package com.tapiporla.microservices.wallet.model

import org.joda.time.DateTime

case class Purchase(
                     date: DateTime,
                     quantity: Int,
                     pricePerShare: BigDecimal,
                     fee: BigDecimal
                   )
