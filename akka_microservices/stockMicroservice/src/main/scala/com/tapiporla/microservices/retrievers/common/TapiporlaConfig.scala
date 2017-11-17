package com.tapiporla.microservices.retrievers.common

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.{Duration, FiniteDuration}

object TapiporlaConfig {

  private val config: Config = ConfigFactory.load()

  private val daemonConfig = config.getConfig("tapiporla.daemon")
  private val elasticConfig = config.getConfig("tapiporla.elasticsearch")
  private val scrapyConfig = config.getConfig("tapiporla.scrapyrt")
  private val statsConfig = config.getConfig("tapiporla.stats")

  //Be careful, this variable affects to Scrapy requests, and parsing responses...
  val globalTimeFormat: String = "dd-MM-yyyy"

  object Daemon{
    val periodicExecution: FiniteDuration = Duration.fromNanos(daemonConfig.getDuration("periodic-execution").toNanos)
    val timeBeforeRetries: FiniteDuration = Duration.fromNanos(daemonConfig.getDuration("time-before-retries").toNanos)
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

  object Stats {
    import scala.collection.JavaConverters._
    val mmGeneration: Seq[Int] = statsConfig.getIntList("mmGeneration").asScala.map(_.intValue())
  }

  object Stock {
    import scala.collection.JavaConverters._

    //Stocks we want to collect with out actors

    val stocks: Seq[StockConfig] =
      ConfigFactory.load("stocks.conf").getConfigList("tapiporla.stocks").asScala.map{ stockConfig =>
        StockConfig(
          stockConfig.getString("name"),
          stockConfig.getString("elasticIndex"),
          stockConfig.getString("crawler"),
          stockConfig.getString("crawlerPath")
        )
      }
  }

  case class StockConfig(name: String, elasticIndex: String, scrapyCrawler: String, crawlerPath: String)

}
