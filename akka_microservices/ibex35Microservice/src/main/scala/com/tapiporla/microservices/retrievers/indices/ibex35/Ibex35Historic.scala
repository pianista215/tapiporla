package com.tapiporla.microservices.retrievers.indices.ibex35

case class Ibex35Historic(
                           date: String,
                           closeValue: BigDecimal,
                           minValue: BigDecimal,
                           maxValue: BigDecimal
                         )
