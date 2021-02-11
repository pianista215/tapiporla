package com.tapiporla.microservices.wallet.model

import org.joda.time.DateTime


case class Dividend(
                     date: DateTime,
                     profit: BigDecimal,
                     fee: BigDecimal
                   )