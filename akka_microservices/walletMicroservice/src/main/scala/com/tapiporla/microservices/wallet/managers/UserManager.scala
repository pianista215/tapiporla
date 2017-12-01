package com.tapiporla.microservices.wallet.managers

import akka.actor.Props
import com.tapiporla.microservices.wallet.common.TapiporlaActor
import com.tapiporla.microservices.wallet.managers.UserManager.{CheckUserExists, UserExistsConfirmation}

object UserManager {

  case class CheckUserExists(userId: String) //TODO: Is valid? Check password? Get By token? Retrieve the full User?
  case class UserExistsConfirmation(userId: String, exists: Boolean)

  def props(): Props = Props(new UserManager())
}

class UserManager extends TapiporlaActor{
  override def receive = mock

  def mock: Receive = {
    case CheckUserExists(userId) =>
      sender() ! UserExistsConfirmation(userId, true)
  }

}
