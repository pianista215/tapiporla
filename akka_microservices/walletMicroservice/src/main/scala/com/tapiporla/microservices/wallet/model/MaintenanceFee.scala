package com.tapiporla.microservices.wallet.model

import org.joda.time.DateTime

case class MaintenanceFee(
                           date: DateTime,
                           fee: BigDecimal
                         )
