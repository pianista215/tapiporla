package com.tapiporla.microservices.wallet.managers

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.routing.{ActorRefRoutee, Router, SmallestMailboxRoutingLogic}
import akka.util.Timeout
import com.tapiporla.microservices.wallet.common.TapiporlaActor
import com.tapiporla.microservices.wallet.managers.UserManager.{CheckUserExists, UserExistsConfirmation}
import com.tapiporla.microservices.wallet.managers.WalletManager.OperationWithUser

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object WalletManager {

  trait OperationWithUser {
    def userId: String
  }



  def props(userRouter: ActorRef): Props = Props(new WalletManager(userRouter))
}

class WalletManager(userRouter: ActorRef) extends TapiporlaActor {

  implicit val timeout = Timeout(20 seconds) //TODO: To Config

  val walletOperatorsRouter = {
    val routees = Vector.fill(1) { //TODO: Config
      val r = context.actorOf(WalletOperator.props())
      context watch r
      ActorRefRoutee(r)
    }
    Router(SmallestMailboxRoutingLogic(), routees)
  }

  override def receive = {

    //TODO: Error management and what happens if not exists???
    case op: OperationWithUser =>
      log.debug(s"Checking user ${op.userId}")
      val currentSender = sender()
      for {
        x <- getUserExists(op.userId)
        if x
      } yield walletOperatorsRouter.route(op, currentSender)

  }

  def getUserExists(userId: String): Future[Boolean] =
    (userRouter ? CheckUserExists(userId)).mapTo[UserExistsConfirmation] map {
      case UserExistsConfirmation(_, exists) => exists
    }

}
