// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import com.microsoft.azure.iot.iothub2cassandra.JsonSerializationProtocol._
import com.microsoft.azure.iot.iothub2cassandra.models.TableSchema
import com.microsoft.azure.iot.iothub2cassandra.webservice.Status
import spray.json._

trait IWebService {
  def start(): Unit
}

case class Webservice(implicit val dependencies: IDependencies) extends IWebService with SprayJsonSupport {

  import dependencies.{cassandraKeyspace, config, log, streamingService}

  implicit val sys = dependencies.system
  implicit val mat = dependencies.materializer

  def start(): Unit = {
    Http().bindAndHandle(routes, config.httpInterface, config.httpPort)
    println(s"Listening on ${config.httpInterface} port ${config.httpPort} ...")
  }

  protected val routes =
    pathPrefix("status") {
      get {
        log.info("/status executed")
        complete(Status.get)
      }
    } ~ pathPrefix("api") {
      path("tables") {
        (post & entity(as[TableSchema])) {
          tableSchema ⇒
            complete {
              cassandraKeyspace.addTable(tableSchema)
              streamingService.restart()
              tableSchema.toJson.prettyPrint
            }
        } ~
          (get) {
            complete(cassandraKeyspace.getTables.map(t ⇒ t._2))
          }
      } ~ pathPrefix("streaming") {
        path("start") {
          (post) {
            streamingService.start()
            complete("Streaming started")
          }
        } ~
          path("stop") {
            (post) {
              streamingService.stop()
              complete("Streaming stopped")
            }
          } ~
          path("restart") {
            (post) {
              streamingService.restart()
              complete("Streaming restarted")
            }
          }
      }
    }
}
