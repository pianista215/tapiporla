package com.tapiporla.microservices.wallet.model

import com.sksamuel.elastic4s.Hit
import com.tapiporla.microservices.wallet.common.ElasticDAO
import com.tapiporla.microservices.wallet.common.model.ElasticDocumentInsertable
import com.tapiporla.microservices.wallet.dao.WalletESDAO

object UserEquity {
  def fromHit(t: Hit): UserEquity = {
    val map = t.sourceAsMap
    UserEquity(
      map(WalletESDAO.user).toString,
      map(WalletESDAO.equity).toString,
      map(WalletESDAO.numberOfShares).toString.toInt,
      BigDecimal(map(WalletESDAO.averageSharePrice).toString),
      map(WalletESDAO.Purchases.id).asInstanceOf[Seq[Map[String,AnyRef]]].map{
        x => Purchase.fromMap(x)
      },
      map(WalletESDAO.Dividends.id).asInstanceOf[Seq[Map[String, AnyRef]]].map{
        x => Dividend.fromMap(x)
      },
      map(WalletESDAO.MaintenanceFees.id).asInstanceOf[Seq[Map[String, AnyRef]]].map{
        x => MaintenanceFee.fromMap(x)
      },
      map.get(ElasticDAO.documentsId).map(_.toString)
    )

    //TODO: Use HitReader?? How to use it with arrays?
  }
}

case class UserEquity(
                       user: String,
                       equity: String,
                       numberOfShares: Int,
                       averageSharePrice: BigDecimal,
                       purchases: Seq[Purchase],
                       dividends: Seq[Dividend],
                       maintenanceFees: Seq[MaintenanceFee],
                       elasticId: Option[String] = None
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

//TODO: Closed operations