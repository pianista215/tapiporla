package com.tapiporla.microservices.retrievers.indices.common

import java.util.Date

case class Ibex35Historic(
                           date: String,
                           closeValue: BigDecimal,
                           minValue: BigDecimal,
                           maxValue: BigDecimal
                         )
