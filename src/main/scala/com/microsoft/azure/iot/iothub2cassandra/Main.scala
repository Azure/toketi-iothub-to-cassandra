// Copyright (c) Microsoft.All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra

object Main extends App {

  implicit val dependencies = Dependencies

  dependencies.webService.start()
  dependencies.streamingService.start()
}
