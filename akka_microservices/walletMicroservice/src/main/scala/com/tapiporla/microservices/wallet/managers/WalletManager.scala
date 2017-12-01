package com.tapiporla.microservices.wallet.managers

import akka.actor.Props
import com.tapiporla.microservices.wallet.common.TapiporlaActor
import com.tapiporla.microservices.wallet.model.{Dividend, MaintenanceFee, Purchase}

object WalletManager {

  case class AddPurchase(userId: String, equityId: String, purchase: Purchase)
  case class AddDividend(userId: String, equityId: String, dividend: Dividend)
  case class AddMaintenanceFee(userId: String, equityId: String, fee: MaintenanceFee)

  def props(): Props = Props(new WalletManager())
}

class WalletManager extends TapiporlaActor{
  override def receive = ???
}
