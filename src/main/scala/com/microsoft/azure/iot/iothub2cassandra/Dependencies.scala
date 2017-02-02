// Copyright (c) Microsoft.All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.microsoft.azure.iot.iothub2cassandra.storage.{IKeyspace, IConnection, Keyspace, Connection}
import com.microsoft.azure.iot.iothubreact.scaladsl.IoTHub

trait IDependencies {
  val system             : ActorSystem
  val materializer       : ActorMaterializer
  val log                : LoggingAdapter
  val config             : IConfig
  val cassandraConnection: IConnection
  val cassandraKeyspace  : IKeyspace
  val streamingService   : IStreamingService
  val webService         : IWebService

  def resetIoTHub(): Unit

  def iotHub(): IoTHub
}

private[iothub2cassandra] object Dependencies extends IDependencies {

  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private[this] var iotHubObj: Option[IoTHub] = None

  def iotHub(): IoTHub = {
    if (!iotHubObj.isDefined) iotHubObj = Some(IoTHub())
    iotHubObj.get
  }

  override def resetIoTHub(): Unit = {
    iotHubObj = None
  }

  lazy val log                 = Logging(system, "iothub2cassandra")
  lazy val config              = new Config
  lazy val cassandraConnection = Connection()
  lazy val cassandraKeyspace   = Keyspace()
  lazy val streamingService    = StreamingService()
  lazy val webService          = Webservice()

  implicit val dependencies: IDependencies = this

  log.info("Cassandra cluster: " + config.cassandraCluster)
  log.info("Web service: " + config.httpInterface + ":" + config.httpPort)
}
