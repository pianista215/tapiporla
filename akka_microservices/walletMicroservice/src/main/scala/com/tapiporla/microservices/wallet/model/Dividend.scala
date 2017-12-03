package com.tapiporla.microservices.wallet.model

import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.wallet.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.wallet.dao.WalletESDAO
import org.joda.time.DateTime

object Dividend {
  def fromMap(map: Map[String, AnyRef]): Dividend = {
    Dividend(
      DateTime.parse(map(WalletESDAO.Dividends.date).toString),
      BigDecimal(map(WalletESDAO.Dividends.profit).toString),
      BigDecimal(map(WalletESDAO.Dividends.fee).toString)
    )
  }
}

case class Dividend(
                     date: DateTime,
                     profit: BigDecimal,
                     fee: BigDecimal
                   ) extends ElasticDocumentInsertable {
  override def json =
    s""" {
       |"${WalletESDAO.Dividends.date}" : "$date",
       |"${WalletESDAO.Dividends.profit}" : "$profit",
       |"${WalletESDAO.Purchases.fee}" : "$fee"
       |} """.stripMargin
}