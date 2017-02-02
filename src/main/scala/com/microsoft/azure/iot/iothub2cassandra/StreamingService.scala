// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra

import java.time.Instant

import akka.stream.scaladsl.{BroadcastHub, Keep}
import com.microsoft.azure.iot.iothub2cassandra.JsonSerializationProtocol._
import com.microsoft.azure.iot.iothub2cassandra.models.{Column, SourceField, TableSchema}
import com.microsoft.azure.iot.iothubreact.MessageFromDevice
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

trait IStreamingService {
  def start(): Unit

  def stop(): Unit

  def restart(): Unit
}

case class StreamingService(implicit val dependencies: IDependencies) extends IStreamingService {

  import dependencies._

  implicit val mat = dependencies.materializer
  implicit val sys = dependencies.system

  // TODO: use a distributed signal to stop the stream
  private[this] var closed = true

  def start(): Unit = {

    if (!cassandraConnection.isReady) {
      log.warning("Cassandra is not available, will retry in 5 seconds")
      system.scheduler.scheduleOnce(5 seconds) {
        start()
      }
    } else {
      if (closed) {
        closed = false
        log.info("Opening stream...")

        val tables = cassandraKeyspace.getTables.toIndexedSeq
        if (tables.size > 0) {

          val runnableGraph = iotHub.source(Instant.now())
            .toMat(BroadcastHub.sink)(Keep.right)
            .run()

          for (i ← 0 until tables.size) runnableGraph.runForeach(prepareAction(tables(i)._2))
        }
      }
    }
  }

  def stop(): Unit = {
    if (!closed) {
      log.info("Closing stream...")
      closed = true
      iotHub.close()
      dependencies.resetIoTHub()
      Thread.sleep(3000)
    }
  }

  def restart(): Unit = {
    stop()
    start()
  }

  /** Create a function to be applied to each IoT message. The function
    * filters, extracts and store records in Cassandra.
    *
    * @param schema Table schema
    *
    * @return Logic to be applied to each message
    */
  private[this] def prepareAction(schema: TableSchema): MessageFromDevice ⇒ Unit = {

    val filter: (MessageFromDevice ⇒ Boolean) = prepareFilter(schema)
    val parser: Map[String, MessageFromDevice ⇒ Option[String]] = prepareMessageParser(schema.columns, schema.contentType)

    (msg: MessageFromDevice) ⇒ {
      if (filter(msg)) {
        val row = parser.mapValues(columnFunction ⇒ columnFunction(msg))
        val data: String = row.toJson.compactPrint

        while (closed) Thread.sleep(1000 * 3)

        cassandraKeyspace.insertRow(schema.keyspace, schema.table, s"JSON '${data}'")
      }
    }
  }

  /** Create a set of functions to extract values from an IoT message
    *
    * @param columns Destination table columns definition
    *
    * @return Set of functions
    */
  private[this] def prepareMessageParser(
      columns: Seq[Column], contentType: Option[String]): Map[String, MessageFromDevice ⇒ Option[String]] = {

    var parser: Map[String, MessageFromDevice ⇒ Option[String]] = Map()

    columns.foreach(column ⇒ {
      parser += column.name → ((msg: MessageFromDevice) ⇒ {
        column.source match {
          case SourceField.None        ⇒ None
          case SourceField.Received    ⇒ Some(msg.created.toString)
          case SourceField.MessageType ⇒ Some(msg.messageType)
          case SourceField.DeviceId    ⇒ Some(msg.deviceId)
          case SourceField.ContentType ⇒ Some(msg.contentType)
          case SourceField.MessageId   ⇒ Some(msg.messageId)

          case SourceField.Content ⇒
            if (column.sourcePath.isEmpty) {
              Some(msg.contentAsString)
            } else {
              if (contentType.isDefined && contentType.get.toLowerCase == "json") {
                val fields: Map[String, JsValue] = msg.contentAsString.parseJson.asJsObject.fields
                val key = column.sourcePath.get
                if (fields.contains(key)) {
                  Some(fields.get(key).get.toString)
                } else {
                  None
                }
              } else {
                Some("unknown content type, unable to parse")
              }
            }

          case SourceField.Properties ⇒
            if (column.sourcePath.isEmpty) {
              Some(msg.properties.asScala.toMap.toJson.toString())
            } else {
              val key = column.sourcePath.get
              if (msg.properties.containsKey(key)) Some(msg.properties.get(key)) else None
            }

          case _ ⇒ {
            log.error(s"Unknown source field `${column.source}`")
            None
          }
        }
      })
    })

    parser
  }

  /** Create a filter for the incoming messages, so that only
    * desired messages are stored in the destination table
    *
    * @param schema Table schema containing the filter rules
    *
    * @return Filtering logic
    */
  private[this] def prepareFilter(schema: TableSchema): MessageFromDevice ⇒ Boolean = {

    if (schema.filters.isDefined) {
      val typeFilter = schema.filters.get.messageType
      val deviceFilter = schema.filters.get.deviceId

      if (typeFilter.isDefined && deviceFilter.isDefined) {
        msg ⇒ (msg.messageType == typeFilter.get && msg.deviceId == deviceFilter.get)
      } else if (typeFilter.isDefined) {
        msg ⇒ (msg.messageType == typeFilter.get)
      } else if (deviceFilter.isDefined) {
        msg ⇒ (msg.deviceId == deviceFilter.get)
      } else {
        msg ⇒ true
      }
    } else {
      msg ⇒ true
    }
  }
}
