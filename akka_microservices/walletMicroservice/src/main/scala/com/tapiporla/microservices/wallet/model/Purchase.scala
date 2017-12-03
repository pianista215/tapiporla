package com.tapiporla.microservices.wallet.model

import com.tapiporla.microservices.wallet.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.wallet.dao.WalletESDAO
import org.joda.time.DateTime

object Purchase {
  def fromMap(map: Map[String, AnyRef]): Purchase = {
    Purchase(
      DateTime.parse(map(WalletESDAO.Purchases.date).toString),
      map(WalletESDAO.Purchases.quantity).toString.toInt,
      BigDecimal(map(WalletESDAO.Purchases.pricePerShare).toString),
      BigDecimal(map(WalletESDAO.Purchases.fee).toString)
    )
  }
}

case class Purchase(
                     date: DateTime,
                     quantity: Int,
                     pricePerShare: BigDecimal,
                     fee: BigDecimal
                   ) extends ElasticDocumentInsertable {
  override def json =
    s""" {
       |"${WalletESDAO.Purchases.date}" : "$date",
       |"${WalletESDAO.Purchases.quantity}" : "$quantity",
       |"${WalletESDAO.Purchases.pricePerShare}" : "$pricePerShare",
       |"${WalletESDAO.Purchases.fee}" : "$fee"
       |} """.stripMargin
}
