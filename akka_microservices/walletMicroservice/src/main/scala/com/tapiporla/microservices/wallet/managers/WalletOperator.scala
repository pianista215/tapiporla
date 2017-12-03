package com.tapiporla.microservices.wallet.managers

import akka.actor.Props
import com.tapiporla.microservices.wallet.common.TapiporlaActor
import com.tapiporla.microservices.wallet.dao.WalletESDAO
import com.tapiporla.microservices.wallet.managers.WalletManager.OperationWithUser
import com.tapiporla.microservices.wallet.managers.WalletOperator.{AddDividend, AddMaintenanceFee, AddPurchase}
import com.tapiporla.microservices.wallet.model.{Dividend, MaintenanceFee, Purchase, UserEquity}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.pipe
import akka.pattern.ask
import akka.util.Timeout
import com.tapiporla.microservices.wallet.common.ElasticDAO.{DataRetrieved, RetrieveAllFromIndexSorted, Update, UpdateConfirmation}

object WalletOperator {
  case class AddPurchase(userId: String, equityId: String, purchase: Purchase) extends OperationWithUser
  case class AddDividend(userId: String, equityId: String, dividend: Dividend) extends OperationWithUser
  case class AddMaintenanceFee(userId: String, equityId: String, fee: MaintenanceFee) extends OperationWithUser

  case class SaveSuccess(userId: String, equityId: String, newState: UserEquity, originalRQ: Product)
  def props(): Props = Props(new WalletOperator())
}

class WalletOperator extends TapiporlaActor {

  implicit val timeout = Timeout(5 seconds) //TODO: To Config

  val esDAO =
    context.actorOf(WalletESDAO.props(), name=s"WalletESDAO_${self.path.name}")

  //TODO: Validations (negative purchases... etc)
  //TODO: Way to handle concurrency (an Actor is specialized in only a shard of users?)
  //TODO: Eventual Consistent?
  override def receive = {
    case AddPurchase(userId, equityId, purchase) =>
      log.info(s"AddPurchase received $userId $equityId $purchase")
      val f = for {
        oldUserEquity <- retrieveUserInfo(userId, equityId)
        newUserEquity <- saveNewUserEquity(addPurchase(oldUserEquity, purchase))
      } yield newUserEquity
      f pipeTo sender


    case AddDividend(userId, equityId, dividend) =>
      log.info(s"AddDividend received $userId $equityId $dividend")
      val f = for {
        oldUserEquity <- retrieveUserInfo(userId, equityId)
        newUserEquity <- saveNewUserEquity(addDividend(oldUserEquity, dividend))
      } yield newUserEquity
      f pipeTo sender

    case AddMaintenanceFee(userId, equityId, fee) =>
      log.info(s"AddMaintenanceFee received $userId $equityId $fee")
      val f = for {
        oldUserEquity <- retrieveUserInfo(userId, equityId)
        newUserEquity <- saveNewUserEquity(addMaintenanceFee(oldUserEquity, fee))
      } yield newUserEquity
      f pipeTo sender
  }

  private def addPurchase(oldUserEquity: UserEquity, purchase: Purchase): UserEquity =
    oldUserEquity.copy(
      numberOfShares = oldUserEquity.numberOfShares + purchase.quantity,
      purchases = oldUserEquity.purchases :+ purchase
    )

  private def addDividend(oldUserEquity: UserEquity, dividend: Dividend): UserEquity =
    oldUserEquity.copy(
      dividends = oldUserEquity.dividends :+ dividend
    )

  private def addMaintenanceFee(oldUserEquity: UserEquity, fee: MaintenanceFee): UserEquity =
    oldUserEquity.copy(
      maintenanceFees = oldUserEquity.maintenanceFees :+ fee
    )

  import com.sksamuel.elastic4s.ElasticDsl._

  def retrieveUserInfo(userId: String, equityId: String): Future[UserEquity] =
    (esDAO ? RetrieveAllFromIndexSorted(
      WalletESDAO.esIndex,
      WalletESDAO.typeName,
      Some(
        boolQuery().must(
          termQuery(WalletESDAO.user, userId),
          termQuery(WalletESDAO.equity, equityId)
        )
      ),
      None,
      Some(1)
    )).mapTo[DataRetrieved].map{ retrieved =>
      log.debug(s"Find in ES, data: $retrieved")
      UserEquity.fromHit(retrieved.data.hits.head)
    }

  def saveNewUserEquity(newState: UserEquity): Future[UserEquity] =
    (esDAO ? Update(
      WalletESDAO.esIndex,
      WalletESDAO.typeName,
      newState.elasticId.get,
      newState
    )).mapTo[UpdateConfirmation].map { retrieved =>
      log.debug(s"Document updated: $newState")
      newState
    }

    //TODO: Error management, in save, etc.... What to do? Messages?
}
