package com.tapiporla.microservices.wallet.common

import scala.math.BigDecimal.RoundingMode

object MathUtils {

  def normalize(b: BigDecimal) = b.bigDecimal.setScale(2, RoundingMode.HALF_UP)
}
