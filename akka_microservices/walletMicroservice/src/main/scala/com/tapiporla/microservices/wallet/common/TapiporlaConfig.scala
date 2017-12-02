package com.tapiporla.microservices.wallet.common

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.{Duration, FiniteDuration}

object TapiporlaConfig {

  private val config: Config = ConfigFactory.load()

  private val daemonConfig = config.getConfig("tapiporla.daemon")
  private val elasticConfig = config.getConfig("tapiporla.elasticsearch")

  //Be careful, this variable affects to Scrapy requests, and parsing responses...
  val globalTimeFormat: String = "dd-MM-yyyy"

  object Daemon{
    val timeBeforeRetries: FiniteDuration = Duration.fromNanos(daemonConfig.getDuration("time-before-retries").toNanos)
  }

  object ElasticSearch {
    val endpoint: String = elasticConfig.getString("endpoint")
    val clusterName: String = elasticConfig.getString("cluster-name")
    val authUser: String = elasticConfig.getString("auth-user")
    val timeBeforeRetries: FiniteDuration = Duration.fromNanos(elasticConfig.getDuration("time-before-retries").toNanos)
  }

}
