package com.tapiporla.microservices.wallet.common

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.{Duration, FiniteDuration}

object TapiporlaConfig {

  private val config: Config = ConfigFactory.load()

  private val daemonConfig = config.getConfig("tapiporla.daemon")

  //Be careful, this variable affects to Scrapy requests, and parsing responses...
  val globalTimeFormat: String = "dd-MM-yyyy"

  object Daemon{
    val timeBeforeRetries: FiniteDuration = Duration.fromNanos(daemonConfig.getDuration("time-before-retries").toNanos)
  }

}
