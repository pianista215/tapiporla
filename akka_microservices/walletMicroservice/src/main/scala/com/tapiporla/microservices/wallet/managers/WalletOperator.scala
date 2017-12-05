package com.tapiporla.microservices.wallet.managers

import akka.actor.{Props, Stash}
import com.tapiporla.microservices.wallet.common.TapiporlaActor
import com.tapiporla.microservices.wallet.dao.WalletESDAO
import com.tapiporla.microservices.wallet.managers.WalletManager.OperationWithUser
import com.tapiporla.microservices.wallet.managers.WalletOperator.{AddDividend, AddMaintenanceFee, AddPurchase, ReleaseUser}
import com.tapiporla.microservices.wallet.model.{Dividend, MaintenanceFee, Purchase, UserEquity}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.pipe
import akka.pattern.ask
import akka.util.Timeout
import com.tapiporla.microservices.wallet.common.ElasticDAO.{DataRetrieved, RetrieveAllFromIndexSorted, Upsert, UpsertConfirmation}

object WalletOperator {
  case class AddPurchase(userId: String, equityId: String, purchase: Purchase) extends OperationWithUser
  case class AddDividend(userId: String, equityId: String, dividend: Dividend) extends OperationWithUser
  case class AddMaintenanceFee(userId: String, equityId: String, fee: MaintenanceFee) extends OperationWithUser

  private case class ReleaseUser(userId: String, equityId: String)

  case class SaveSuccess(userId: String, equityId: String, newState: UserEquity, originalRQ: Product)
  def props(): Props = Props(new WalletOperator())
}

class WalletOperator extends TapiporlaActor with Stash {

  implicit val timeout = Timeout(20 seconds) //TODO: To Config

  val esDAO =
    context.actorOf(WalletESDAO.props(), name=s"WalletESDAO_${self.path.name}")

  override def receive =
    ready(Set.empty)

  //TODO: Validations (negative purchases... etc)
  //TODO: Way to handle concurrency (an Actor is specialized in only a shard of users?)
  //TODO: Eventual Consistent?
  def ready(blockedUserEquities: Set[(String,String)]): Receive = {

    case rq@AddPurchase(userId, equityId, purchase) =>
      if(blockedUserEquities contains (userId,equityId)){
        log.info("User blocked. Waiting to unlock, before process.")
        stash()
      } else {
        log.info(s"AddPurchase received $userId $equityId $purchase")
        val f = for {
          oldUserEquity <- retrieveUserInfo(userId, equityId)
          newUserEquity <- saveNewUserEquity(addPurchase(oldUserEquity, purchase))
        } yield newUserEquity
        f onComplete {_ => self ! ReleaseUser(userId, equityId)}
        f pipeTo sender
        context.become(ready(blockedUserEquities + ((userId, equityId))))
      }



    case rq@AddDividend(userId, equityId, dividend) =>
      if(blockedUserEquities contains (userId,equityId)){
        log.info("User blocked. Waiting to unlock, before process.")
        stash()
      } else {
        log.info(s"AddDividend received $userId $equityId $dividend")
        val f = for {
          oldUserEquity <- retrieveUserInfo(userId, equityId)
          newUserEquity <- saveNewUserEquity(addDividend(oldUserEquity, dividend))
        } yield newUserEquity
        f onComplete {_ => self !ReleaseUser(userId, equityId)}
        f pipeTo sender
        context.become(ready(blockedUserEquities + ((userId, equityId))))
      }

    case rq@AddMaintenanceFee(userId, equityId, fee) =>
      if(blockedUserEquities contains (userId,equityId)){
        log.info("User blocked. Waiting to unlock, before process.")
        stash()
      } else {
        log.info(s"AddMaintenanceFee received $userId $equityId $fee")
        val f = for {
          oldUserEquity <- retrieveUserInfo(userId, equityId)
          newUserEquity <- saveNewUserEquity(addMaintenanceFee(oldUserEquity, fee))
        } yield newUserEquity
        f onComplete {_ => self !ReleaseUser(userId, equityId)}
        f pipeTo sender
        context.become(ready(blockedUserEquities + ((userId, equityId))))
      }



    case ReleaseUser(userId, equityId) =>
      log.info(s"Releasing user: $userId - $equityId")
      unstashAll()
      context.become(ready(blockedUserEquities - ((userId, equityId))))
  }

  //TODO: Logic validations
  private def addPurchase(oldUserEquity: UserEquity, purchase: Purchase): UserEquity =
    oldUserEquity.withPurchase(purchase)

  private def addDividend(oldUserEquity: UserEquity, dividend: Dividend): UserEquity =
    oldUserEquity.withDividend(dividend)

  private def addMaintenanceFee(oldUserEquity: UserEquity, fee: MaintenanceFee): UserEquity =
    oldUserEquity.withMaintenanceFee(fee)

  import com.sksamuel.elastic4s.ElasticDsl._
  import com.tapiporla.microservices.wallet.common.CustomElasticJackson.Implicits._

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
      retrieved.data.hits.headOption.map { hit =>
        hit.safeTo[UserEquity] match{
          case Left(e) =>
            log.error(e, "Error parsing to UserEquity from ES")
            throw e
          case Right(ue) =>
            ue
        }
      }.getOrElse(UserEquity.empty(userId, equityId))
    }

  def saveNewUserEquity(newState: UserEquity): Future[UserEquity] =
    (esDAO ? Upsert(
      WalletESDAO.esIndex,
      WalletESDAO.typeName,
      newState.getElasticId,
      newState
    )).mapTo[UpsertConfirmation].map { retrieved =>
      log.debug(s"Document updated: $newState")
      newState
    }

    //TODO: Error management, in save, etc.... What to do? Messages?
}
