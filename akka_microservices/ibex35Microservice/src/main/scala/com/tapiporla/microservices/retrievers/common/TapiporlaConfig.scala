package com.tapiporla.microservices.retrievers.common

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.{Duration, FiniteDuration}

object TapiporlaConfig {

  private val config: Config = ConfigFactory.load()

  private val daemonConfig = config.getConfig("tapiporla.daemon")
  private val elasticConfig = config.getConfig("tapiporla.elasticsearch")
  private val scrapyConfig = config.getConfig("tapiporla.scrapyrt")

  object Daemon{
    val periodicExecution: FiniteDuration = Duration.fromNanos(daemonConfig.getDuration("periodic-execution").toNanos)
  }

  object ElasticSearch {
    val endpoint: String = elasticConfig.getString("endpoint")
    val clusterName: String = elasticConfig.getString("cluster-name")
    val authUser: String = elasticConfig.getString("auth-user")
    val timeBeforeRetries: FiniteDuration = Duration.fromNanos(elasticConfig.getDuration("time-before-retries").toNanos)
  }

  object ScrapyRT {
    val endpoint: String = scrapyConfig.getString("endpoint")
  }

}
