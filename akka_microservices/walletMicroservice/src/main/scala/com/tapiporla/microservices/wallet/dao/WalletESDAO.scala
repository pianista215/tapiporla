package com.tapiporla.microservices.wallet.dao

import akka.actor.Props
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.tapiporla.microservices.wallet.common.ElasticDAO

object WalletESDAO {

  val esIndex = "user_equities"
  val typeName = "user_equity"

  val user = "user"
  val equity = "equity"
  val numberOfShares = "number_of_shares"
  val averageSharePrice = "average_share_price"

  object Purchases {
    val id = "purchases"
    val date = "date"
    val quantity = "quantity"
    val pricePerShare = "price_per_share"
    val fee = "fee"
  }

  object Dividends {
    val id = "dividends"
    val date = "date"
    val profit = "profit"
    val fee = "fee"
  }

  object MaintenanceFees {
    val id = "maintenance_fees"
    val date = "date"
    val fee = "fee"
  }

  object Report {
    val id = "report"
    val baseCost = "base_cost"
    val totalFees = "total_fees"
    val finalCost = "final_cost"
    val salePrice = "sale_price"
    val grossProfit = "gross_profit"
    val dividendsProfit = "dividends_profit"
    val netProfit = "net_profit"
    val percentage = "percentage"
  }

  def props(): Props = Props(new WalletESDAO())
}

class WalletESDAO extends ElasticDAO {



  override def indexCreation: CreateIndexDefinition = {
    createIndex(WalletESDAO.esIndex).mappings(

      mapping(WalletESDAO.typeName) as(

        keywordField(WalletESDAO.user),
        keywordField(WalletESDAO.equity),

        intField(WalletESDAO.numberOfShares),
        doubleField(WalletESDAO.averageSharePrice),

        nestedField(WalletESDAO.Purchases.id) fields (
          dateField(WalletESDAO.Purchases.date),
          intField(WalletESDAO.Purchases.quantity),
          doubleField(WalletESDAO.Purchases.pricePerShare),
          doubleField(WalletESDAO.Purchases.fee)
        ),

        nestedField(WalletESDAO.Dividends.id) fields (
          dateField(WalletESDAO.Dividends.date),
          doubleField(WalletESDAO.Dividends.profit),
          doubleField(WalletESDAO.Dividends.fee)
        ),

        nestedField(WalletESDAO.MaintenanceFees.id) fields (
          dateField(WalletESDAO.MaintenanceFees.id),
          doubleField(WalletESDAO.MaintenanceFees.fee)
        ),

        /*objectField(WalletESDAO.Report.id) fields (
          doubleField(WalletESDAO.Report.baseCost),
          doubleField(WalletESDAO.Report.totalFees),
          doubleField(WalletESDAO.Report.finalCost),
          doubleField(WalletESDAO.Report.salePrice),
          doubleField(WalletESDAO.Report.grossProfit),
          doubleField(WalletESDAO.Report.dividendsProfit),
          doubleField(WalletESDAO.Report.netProfit),
          doubleField(WalletESDAO.Report.percentage)
        )*/

      )

    )
  }
}