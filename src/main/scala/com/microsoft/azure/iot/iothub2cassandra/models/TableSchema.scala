// Copyright (c) Microsoft.All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra.models

import com.microsoft.azure.iot.iothub2cassandra.models.KeyType.KeyType
import com.microsoft.azure.iot.iothub2cassandra.models.SourceField.SourceField

case class TableSchema(
    var keyspace: String,
    var table: String,
    contentType: Option[String],
    filters: Option[Filters],
    columns: Seq[Column]) {

  keyspace = filterName(keyspace)
  table = filterName(table)
  val this.filters = if (filters.isDefined) filters else None

  private[this] def filterName(name: String): String = {
    val x: String = name.replaceAll("[^\\p{IsAlphabetic}^\\p{IsDigit}]", "_")
    if (x(0).isDigit) '_' + x else x
  }
}

case class Column(
    name: String,
    `type`: Option[String],
    source: SourceField,
    sourcePath: Option[String],
    key: Option[KeyType]) {

  val this.`type`     = if (`type`.isDefined) `type` else Some("text")
  val this.key        = if (key.isDefined) key else None
  val this.sourcePath = if (sourcePath.isDefined) sourcePath else None
  val this.name       = filterName(name)

  private[this] def filterName(name: String): String = {
    val x: String = name.replaceAll("[^\\p{IsAlphabetic}^\\p{IsDigit}]", "_")
    if (x(0).isDigit) '_' + x else x
  }
}

case class Filters(
    messageType: Option[String] = None,
    deviceId: Option[String] = None
) {
  val this.messageType = if (messageType.isDefined) messageType else None
  val this.deviceId    = if (deviceId.isDefined) deviceId else None
}

object SourceField extends Enumeration {
  type SourceField = Value
  val None        = Value("None")
  val Received    = Value("Received")
  val MessageType = Value("MessageType")
  val MessageId   = Value("MessageId")
  val DeviceId    = Value("DeviceId")
  val Content     = Value("Content")
  val Properties  = Value("Properties")
  val ContentType = Value("ContentType")
}

object KeyType extends Enumeration {
  type KeyType = Value
  val None       = Value("None")
  val Partition  = Value("Partition")
  val Clustering = Value("Clustering")
}
