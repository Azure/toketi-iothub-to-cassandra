// Copyright (c) Microsoft.All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra.storage

import com.datastax.driver.core.exceptions.NoHostAvailableException
import com.datastax.driver.core.{Cluster, Session}
import com.microsoft.azure.iot.iothub2cassandra.IDependencies
import com.microsoft.azure.iot.iothub2cassandra.models.CassandraNotAvailableException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

trait IConnection {
  def getSession: Session

  def reconnect(): Unit

  def isReady: Boolean
}

private[iothub2cassandra] case class Connection(implicit val dependencies: IDependencies) extends IConnection {

  import dependencies.{config, log, system}

  private[this] val hostPort         = extractHostPort()
  private[this] var cluster: Cluster = null
  private[this] var readyStatus      = false
  private[this] var session: Session = null

  connect()

  def isReady: Boolean = readyStatus

  def getSession: Session = {
    if (!isReady) connect()
    if (!isReady) throw new CassandraNotAvailableException
    session
  }

  def reconnect(): Unit = {
    readyStatus = false
    connect()
  }

  private[this] def connect(): Unit = {
    try {
      cluster = Cluster.builder().addContactPoint(hostPort._1).withPort(hostPort._2).build()
      session = cluster.connect()
      readyStatus = true
    } catch {
      case e: NoHostAvailableException ⇒ {
        cluster.close()
        log.error("Unable to connect to Cassandra: " + e)

        system.scheduler.scheduleOnce(5 seconds) {
          connect()
        }
      }
    }
  }

  private[this] def extractHostPort(): (String, Int) = {
    val tokens = config.cassandraCluster.split(":")
    (tokens(0), if (tokens.size == 2) tokens(1).toInt else 9042)
  }
}
