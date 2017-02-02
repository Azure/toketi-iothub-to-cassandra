// Copyright (c) Microsoft.All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra

import com.microsoft.azure.iot.iothub2cassandra.models.CassandraNotAvailableException
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}

import scala.collection.JavaConverters._

trait IConfig {

  val defaultHttpInterface  = "0.0.0.0"
  val defaultHttpPort       = 9000
  val defaultConfigKeyspace = "iothub2cassandra"
  val defaultConfigTable    = "tables"

  val httpInterface : String
  val httpPort      : Int
  val configKeyspace: String
  val configTable   : String

  // TODO: support list, use Seq[String]
  val cassandraCluster: String
  val tables          : Seq[String]
}

private[iothub2cassandra] class Config extends IConfig {

  System.clearProperty("config.url")
  System.clearProperty("config.file")

  if (sys.env.contains("CONFIG_FILE")) {
    System.setProperty("config.file", sys.env("CONFIG_FILE"))
    ConfigFactory.invalidateCaches()
  }

  if (sys.env.contains("CONFIG_URL")) {
    System.setProperty("config.url", sys.env("CONFIG_URL"))
    ConfigFactory.invalidateCaches()
  }

  private[this] val config          = ConfigFactory.load()
  private[this] val cassandraConfig = config.getConfig("cassandra")
  private[this] val serviceConfig   = config.getConfig("iothub2cassandra")

  if (!cassandraConfig.hasPath("cluster")) throw new CassandraNotAvailableException("Unable to locate Cassandra configuration")
  val cassandraCluster = cassandraConfig.getString("cluster")

  val httpInterface = if (!serviceConfig.hasPath("http.interface")) defaultHttpInterface else
    serviceConfig.getString("http.interface")

  val httpPort = if (!serviceConfig.hasPath("http.port")) defaultHttpPort else
    serviceConfig.getInt("http.port")

  val configKeyspace = if (!serviceConfig.hasPath("configKeyspace")) defaultConfigKeyspace else
    serviceConfig.getString("configKeyspace")

  val configTable = if (!serviceConfig.hasPath("configTable")) defaultConfigTable else
    serviceConfig.getString("configTable")

  val tables = if (!serviceConfig.hasPath("tables")) Seq() else
    serviceConfig.getList("tables").asScala.map(x ⇒ x.render(ConfigRenderOptions.concise()))
}
