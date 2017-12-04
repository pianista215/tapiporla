package com.tapiporla.microservices.wallet

import akka.actor.{ActorRef, Props}
import akka.routing.FromConfig
import com.tapiporla.microservices.wallet.BossCoordinator.StartAllCoordinators
import com.tapiporla.microservices.wallet.common.TapiporlaActor
import com.tapiporla.microservices.wallet.managers.WalletOperator.{AddDividend, AddMaintenanceFee, AddPurchase}
import com.tapiporla.microservices.wallet.managers.{UserManager, WalletManager}
import com.tapiporla.microservices.wallet.model.{Dividend, MaintenanceFee, Purchase}
import org.joda.time.DateTime



object BossCoordinator {

  def props(): Props = Props(new BossCoordinator())

  case object StartAllCoordinators
}

class BossCoordinator extends TapiporlaActor {


  override def receive = initial

  def initial: Receive = {

    case StartAllCoordinators =>
      log.info("Initializing BossCoordinator")

      val userRouter =
        context.actorOf(FromConfig.props(UserManager.props()), "userRouter")
      val walletRouter =
        context.actorOf(FromConfig.props(WalletManager.props(userRouter)), "walletRouter")

      walletRouter ! AddPurchase("prueba", "IAG", Purchase(DateTime.now(), 200, 15.0, 15.0))
      walletRouter ! AddDividend("prueba", "IAG", Dividend(DateTime.now(), 30.0, 1.0))
      walletRouter ! AddMaintenanceFee("prueba", "IAG", MaintenanceFee(DateTime.now(), 5.0))

      context.become(started(userRouter, walletRouter))

  }

  def started(userRouter: ActorRef, walletRouter: ActorRef): Receive = {
    case msg => unhandled(msg)
  }


}

