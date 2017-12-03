package com.tapiporla.microservices.wallet.model

import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.wallet.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.wallet.dao.WalletESDAO
import org.joda.time.DateTime

object MaintenanceFee {
  def fromMap(map: Map[String, AnyRef]): MaintenanceFee = {
    MaintenanceFee(
      DateTime.parse(map(WalletESDAO.MaintenanceFees.date).toString),
      BigDecimal(map(WalletESDAO.MaintenanceFees.fee).toString)
    )
  }
}

case class MaintenanceFee(
                           date: DateTime,
                           fee: BigDecimal
                         ) extends ElasticDocumentInsertable {

  override def json =
    s""" {
       |"${WalletESDAO.Purchases.date}" : "$date",
       |"${WalletESDAO.Purchases.fee}" : "$fee"
       |} """.stripMargin
}
