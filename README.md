# Azure IoTHub to Cassandra connector

*Azure IoTHub to Cassandra* is a connector that allows to transfer data in real time, **from
[Azure IoT Hub](https://azure.microsoft.com/en-us/services/iot-hub/) to 
[Apache Cassandra](http://cassandra.apache.org/)**. Every message sent from a device connected to 
Azure IoT Hub, is automatically copied into one or more Cassandra tables. 

[Azure IoT Hub](https://azure.microsoft.com/en-us/services/iot-hub/) is a service used to connect 
thousands to millions of devices to the Azure cloud. Each device can send telemetry and receive 
messages and commands from the cloud.

[Apache Cassandra](http://cassandra.apache.org/) is a highly scalable, fault tolerant, open source 
database, providing high availability and replication across datacenters. The database is organized
in keyspaces and tables.

*IoTHub to Cassandra* uses a **reactive stream**, with **asynchronous back pressure** to transfer 
IoT messages. Messages can be filtered and mapped to **multiple keyspaces and tables**. New tables
can be added through the configuration, or at runtime using the included web service.

# How to test the connector in 5 minutes

1. [Install Docker](https://docs.docker.com/engine/installation/)
2. [Create an Azure IoT Hub](https://ms.portal.azure.com/#create/Microsoft.IotHub)
3. Download [docker-compose.yaml](docker-compose.yaml) 
4. Download and edit [setup-env-vars.sh](setup-env-vars.sh) (or [setup-env-vars.bat](setup-env-vars.bat) 
   on Windows) storing your hub connection settings
5. Run `setup-env-vars.sh` (or `setup-env-vars.bat` on Windows)
6. Run `docker-compose up`

The last command will download the Docker image publicly available [on Docker Hub]
(https://hub.docker.com/r/toketi/iothub-to-cassandra) and start the connector, logging events on the 
console.

You should have now 2 containers running. Cassandra tables can be queried on port 9042, and 
IoTHub2Cassandra has a web service listening on port 9000.

Optionally: 

1. [Simulate messages](https://github.com/Azure/iothub-explorer) sent to your Azure IoT Hub
2. See the messages flowing to Cassandra:
```
docker exec -it toketiiothubtocassandra_cassandra_1 cqlsh -C -e \
    'SELECT COUNT(*) FROM mykeyspace.full_log'
```

To change the configuration used by Docker Compose, you can edit 
[docker_compose_config_demo.conf](docker_compose_config_demo.conf), which is referenced in 
[docker-compose.yaml](docker-compose.yaml).

# Running the connector

Before running the service, make sure to prepare 

1. an Azure IoT Hub from which to read the data. We recommend to store the settings in 
   environment variables, editing and executing [setup-env-vars.sh](setup-env-vars.sh) 
   (or [setup-env-vars.bat](setup-env-vars.bat) on Windows).
2. a configuration file with the information to connect to Azure IoT Hub and Cassandra (the 
   configuration file included in the repository references the environment variables set in the 
   previous step).

The following Azure IoT Hub documentation will help creating thue hub and find the connection settings:

* [Create an IoT hub using the Azure portal: Endpoints](https://docs.microsoft.com/en-us/azure/iot-hub/iot-hub-create-through-portal#endpoints)
* [Get started with Azure IoT Hub (Java)](https://docs.microsoft.com/en-us/azure/iot-hub/iot-hub-java-java-getstarted)

## Running the service as a Docker container

For a quick test we suggest to use Docker Compose, which takes care of starting an instance of Apache
Cassandra. Just download [docker-compose.yaml](docker-compose.yaml) to your workstation and 
(from the same folder) run:

```
docker-compose up 
```

This will automatically

1. start Cassandra
2. start *IoT Hub to Cassandra* service
3. connect to your Azure IoT hub
4. start streaming messages (depending on the configured tables)

If you prefer storing events in a different Cassandra instance, you can start the [Docker image publicly 
available on Docker Hub](https://hub.docker.com/r/toketi/iothub-to-cassandra/), running the following
command. Note the need for a configuration file, you can use the one in the repository (see the 
[docs](https://github.com/Azure/toketi-iothub-to-cassandra) folder for examples).

```
docker run -it -v $(pwd)/docs:/config -e CONFIG_FILE=/config/application_config_sample.conf \
   -e IOTHUB_EVENTHUB_NAME=$IOTHUB_EVENTHUB_NAME -e IOTHUB_EVENTHUB_ENDPOINT=$IOTHUB_EVENTHUB_ENDPOINT \
   -e IOTHUB_EVENTHUB_PARTITIONS=$IOTHUB_EVENTHUB_PARTITIONS -e IOTHUB_ACCESS_POLICY=$IOTHUB_ACCESS_POLICY \
   -e IOTHUB_ACCESS_KEY=$IOTHUB_ACCESS_KEY \
   -p 9000:9000 toketi/iothub-to-cassandra
```

# Configuration

The following is a minimal configuration file example. Note how values can be replaced with the 
`${?XYZ}` syntax, which allows to retrieve values from the environment (e.g. to avoid visible passwords).

```
iothub {
  hubName        = my-iothub-one
  hubEndpoint    = sb://iothub-ns-myioth-75186-9fb862f912.servicebus.windows.net/
  hubPartitions  = 4
  accessPolicy   = service
  accessKey      = ${?IOTHUB_ACCESS_KEY}
  accessHostName = my-iothub-one.azure-devices.net
}

cassandra {
  cluster = "localhost"
}

http {
  interface = "0.0.0.0"
  port      = 9000
}

iothub2cassandra {
  configTable: "tables"
  configKeyspace: "azureiothub2cassandra"
}
```

A more interesting configuration is available [here](docs/application_config_sample.conf), with other
settings and comments describing their use case. Here's a comprehensive list:

| Setting | Description | Example |
|---------|-------------|---------|
| akka.loglevel                   | Logging level | DEBUG |
| cassandra.cluster               | Cluster to connect | 192.168.0.10 |
| iothub2cassandra.http.interface | Web service IP | 0.0.0.0 |
| iothub2cassandra.http.port      | Web service port | 9000 |
| iothub2cassandra.configTable    | Table where to store the list of tables to populate | tables |
| iothub2cassandra.configKeyspace | Keyspace where to store the service configuration | azureiothub2cassandra |
| iothub2cassandra.tables         | Optional list of tables to populate | *array*, see [this sample](docs/application_config_sample.conf) |
| iothub.hubPartitions            | Number of partition in your Azure IoT Hub | 4 | 
| iothub.hubName                  | Event Hub-compatible name | my-iothub-one |
| iothub.hubEndpoint              | Event Hub-compatible endpoint | sb://iothub-ns-myioth-75186-9fb862f912.servicebus.windows.net/ |
| iothub.accessPolicy             | Access policy to use | service |
| iothub.accessKey                | Access policy secret key | 6XdRSFB9H61f+N3uOdBJiKwzeqbZUj1K//T2jFyewN4= |
| iothub.accessHostName           | Access policy HostName | my-iothub-one.azure-devices.net |

*IoT Hub to Cassandra* is built upon the [Azure IoTHubReact](https://github.com/Azure/toketi-iothubreact) 
library, which have some extra configuration settings. For a complete description, please see  
[IoT Hub React reference file](https://github.com/Azure/toketi-iothubreact/blob/master/src/main/resources/reference.conf) 
included in its repository.

| Setting | Description |
|---------|-------------|
| iothub.*               | Connection details to connect to Azure IoT Hub |
| iothub-stream.*        | Streaming details, e.g. batch size and timeout |
| iothub-checkpointing.* | Checkpointing details, i.e. how to keep the current stream position in case of restart |

# Defining the Cassandra tables

*IoT Hub to Cassandra* allows to stream telemetry to different tables, with different schemas,
filtering data, and extracting data from telemetry (in JSON format).

Tables can be added in two different ways:

1. through the configuration file
2. through the included web service

A table specification allows to:

1. Specify where to write the data
2. The schema of each table, i.e. the column name, the 
   [clustering](http://cassandra.apache.org/doc/latest/cql/ddl.html#the-clustering-columns) 
   and [partitioning keys](http://cassandra.apache.org/doc/latest/cql/ddl.html#the-partition-key)
3. How to map values from the incoming telemetry to the table columns
4. Optional filters, e.g. to filter messages by message type and/or device ID

## Tables added in the configuration file

**Multiple tables** can be added in the configuration file. These tables will be created as soon as
the service starts, and the service will immediately start populating them, reading the telemetry
stream from Azure IoT Hub.

Here's an example of two distinct tables. The first will contain a copy of all the incoming messages. 
The second table instead, will contain only "temperature" messages, extracting the temperature value.
 
Note: in the second table, the source payload must be in JSON format, and the message type must be 
set by the device (adding a `"$$messageType"` custom property).

```
...
  tables: [
    {
      "table":    "full_log",
      "keyspace": "mykeyspace",
      "columns": [
        {"name": "messageType", "type": "text",      "source": "MessageType", "key": "Partition"},
        {"name": "id",          "type": "text",      "source": "MessageId",   "key": "Partition"},
        {"name": "time",        "type": "timestamp", "source": "Received",    "key": "Clustering"},
        {"name": "device",      "type": "text",      "source": "DeviceId",    "key": "Clustering"},
        {"name": "content",     "type": "text",      "source": "Content"}
      ]
    },
    {
      "table":       "livingRoomTemperature",
      "keyspace":    "mykeyspace",
      "contentType": "json",
      "filters": {
        "messageType": "temperature",
        "deviceId": "livingRoom"
      },
      "columns": [
        {"name": "time",   "type": "timestamp", "source": "Received", "key": "Partition"},
        {"name": "device", "type": "text",      "source": "DeviceId", "key": "Partition"},
        {"name": "value",  "type": "double",    "source": "Content",  "sourcePath": "value"}
      ]
    }
  ]
...
```

In the [docs](docs) folder you can find multiple examples, showing how to define 
[partition](http://cassandra.apache.org/doc/latest/cql/ddl.html#the-partition-key) keys,
[clustering](http://cassandra.apache.org/doc/latest/cql/ddl.html#the-clustering-columns) keys, 
columns [types](http://docs.datastax.com/en/cql/3.1/cql/cql_reference/cql_data_types_c.html), 
filters, and how to extract values from the messages payload.

## Adding tables using the web service

The list of tables, and how to write the incoming telemetry into these tables, is stored in a 
configuration table in Cassandra, so you can for example restart and re-deploy the service without
losing this information.

Adding new tables through the configuration file requires to deploy and restart the service, which
is not always possible or desired. *IoTHub to Cassandra* includes a **web service** that allows to 
**add new table configurations at runtime**, without the need to restart.

The syntax used by the web service is equivalent to the one used in the configuration file seen above.
In fact you can copy and paste a table definition, from the configuration file to the webservice, 
and viceversa.

Here's an example of how to add the same `full_log` table seen above, using the web service:

**Add `full_log` table**

Web service request:
```
POST http://<host>:9000/api/tables
Content-Type: application/json

{
  "table":    "full_log",
  "keyspace": "mykeyspace",
  "columns": [
    {"name": "messageType", "type": "text",      "source": "MessageType", "key": "Partition"},
    {"name": "id",          "type": "text",      "source": "MessageId",   "key": "Partition"},
    {"name": "time",        "type": "timestamp", "source": "Received",    "key": "Clustering"},
    {"name": "device",      "type": "text",      "source": "DeviceId",    "key": "Clustering"},
    {"name": "content",     "type": "text",      "source": "Content"}
  ]
}
```

Response:

```
200 OK
```

#### Other web service methods

The web service provides also methods to list the configured tables, to start/stop/restart the streaming,
and to check the service health status.

##### Get a list of all the tables

`GET http://<host>:9000/api/tables`

##### Start/Stop/Restart the streaming

`POST http://<host>:9000/api/streaming/start`

`POST http://<host>:9000/api/streaming/stop`

`POST http://<host>:9000/api/streaming/restart`

##### Check service status

`GET http://<host>:9000/status`

# Contribute Code

If you want/plan to contribute, we ask you to sign a [CLA](https://cla.microsoft.com/) 
(Contribution License Agreement). A friendly bot will remind you about it when you submit 
a pull-request.

If you are sending a pull request, we kindly request to check the code style with IntelliJ IDEA, 
importing the settings from 
[`Codestyle.IntelliJ.xml`](https://github.com/Azure/toketi-iot-tools/blob/dev/Codestyle.IntelliJ.xml).
