package com.tapiporla.microservices.wallet.managers

import akka.actor.Props
import com.tapiporla.microservices.wallet.common.TapiporlaActor
import com.tapiporla.microservices.wallet.managers.WalletManager.OperationWithUser
import com.tapiporla.microservices.wallet.managers.WalletOperator.{AddDividend, AddMaintenanceFee, AddPurchase}
import com.tapiporla.microservices.wallet.model.{Dividend, MaintenanceFee, Purchase}

object WalletOperator {
  case class AddPurchase(userId: String, equityId: String, purchase: Purchase) extends OperationWithUser
  case class AddDividend(userId: String, equityId: String, dividend: Dividend) extends OperationWithUser
  case class AddMaintenanceFee(userId: String, equityId: String, fee: MaintenanceFee) extends OperationWithUser

  def props(): Props = Props(new WalletOperator())
}

class WalletOperator extends TapiporlaActor {

  override def receive = {
    case AddPurchase(userId, equityId, purchase) =>
      log.info("AddPurchase received")

    case AddDividend(userId, equityId, dividend) =>
      log.info("AddDividend received")

    case AddMaintenanceFee(userId, equityId, fee) =>
      log.info("AddMaintenanceFee received")
  }


}
