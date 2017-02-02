// Copyright (c) Microsoft.All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra.webservice

import java.lang.management.ManagementFactory

import spray.json.{JsNumber, JsObject, JsString, JsValue}

object Status {
  def get: JsValue = {
    val runtimeMXB = ManagementFactory.getRuntimeMXBean
    val osMXB = ManagementFactory.getOperatingSystemMXBean

    val runtime = JsObject(
      "startTime" → JsNumber(runtimeMXB.getStartTime),
      "uptimeMsecs" → JsNumber(runtimeMXB.getUptime),
      "name" → JsString(runtimeMXB.getName),
      "vmName" → JsString(runtimeMXB.getVmName)
    )

    val os = JsObject(
      "arch" → JsString(osMXB.getArch),
      "name" → JsString(osMXB.getName),
      "version" → JsString(osMXB.getVersion),
      "processors" → JsNumber(osMXB.getAvailableProcessors),
      "systemLoadAvg" → JsNumber(osMXB.getSystemLoadAverage)
    )

    JsObject(
      "runtime" → runtime,
      "os" → os
    )
  }
}
