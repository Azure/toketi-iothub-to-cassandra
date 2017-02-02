// Copyright (c) Microsoft.All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra

import com.microsoft.azure.iot.iothub2cassandra.models.KeyType.KeyType
import com.microsoft.azure.iot.iothub2cassandra.models.SourceField.SourceField
import com.microsoft.azure.iot.iothub2cassandra.models._
import spray.json._

import scala.language.{implicitConversions, postfixOps}

private[iothub2cassandra] object JsonSerializationProtocol extends DefaultJsonProtocol {

  implicit object SourceFormat extends RootJsonFormat[SourceField] {

    override def write(obj: SourceField): JsValue = JsString(obj.toString)

    override def read(json: JsValue): SourceField = SourceField.withName(json.asInstanceOf[JsString].value)
  }

  implicit object KeyTypeFormat extends RootJsonFormat[KeyType] {

    override def write(obj: KeyType): JsValue = JsString(obj.toString)

    override def read(json: JsValue): KeyType = KeyType.withName(json.asInstanceOf[JsString].value)
  }

  implicit def filtersFormatter = jsonFormat2(Filters)

  implicit def columnFormatter = jsonFormat5(Column)

  implicit def tableSchemaFormatter = jsonFormat5(TableSchema)
}
