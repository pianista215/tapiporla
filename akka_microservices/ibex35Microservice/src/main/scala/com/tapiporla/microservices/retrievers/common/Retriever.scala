package com.tapiporla.microservices.retrievers.common

import akka.actor.{Actor, ActorLogging}


object Retriever {
  object UpdateData //Used to keep data updated
  case class UpdatedOK() //Used to confirm the update
  case class UpdatedError(msg: String) //Used to send errors to the caller
}


trait Retriever extends Actor with ActorLogging {

}
