package com.tapiporla.microservices.wallet.model

import com.tapiporla.microservices.wallet.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.wallet.dao.WalletESDAO
import org.joda.time.DateTime

case class UserEquity(
                       user: String,
                       equity: String,
                       numberOfShares: Int,
                       averageSharePrice: BigDecimal,
                       purchases: Seq[Purchase],
                       dividends: Seq[Dividend],
                       maintenanceFees: Seq[MaintenanceFee]
                     ) extends ElasticDocumentInsertable {

  //TODO: Enhance JSON mapping using a library like (Same as StockMicroservice)
  override def json =
    s""" {
       |"${WalletESDAO.user}" : "$user",
       |"${WalletESDAO.equity}" : "$equity",
       |"${WalletESDAO.numberOfShares}" : "$numberOfShares",
       |"${WalletESDAO.averageSharePrice}" : "$averageSharePrice",
       |"${WalletESDAO.Purchases.id}" : "${purchases.map(_.json).mkString("[",",","]")}",
       |"${WalletESDAO.Dividends.id}" : "${dividends.map(_.json).mkString("[", ",", "]")}",
       |"${WalletESDAO.MaintenanceFees.id}" : "${maintenanceFees.map(_.json).mkString("[", ",", "]")}"
       |} """.stripMargin

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


//TODO: Closed operations