// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iot.iothub2cassandra.storage

import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.exceptions.{AlreadyExistsException, InvalidQueryException, NoHostAvailableException, SyntaxError}
import com.microsoft.azure.iot.iothub2cassandra.IDependencies
import com.microsoft.azure.iot.iothub2cassandra.JsonSerializationProtocol._
import com.microsoft.azure.iot.iothub2cassandra.models.{KeyType, TableSchema}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

trait IKeyspace {
  def addTable(schema: TableSchema): Unit

  def getTables: Map[String, TableSchema]

  def insertRow(keyspace: String, table: String, data: String): Unit
}

private[iothub2cassandra] case class Keyspace(implicit val dependencies: IDependencies) extends IKeyspace {

  import dependencies.{cassandraConnection, config, log, system}

  private val configKeyspace    = config.configKeyspace
  private val configTable       = config.configTable
  private val configTableSchema = s"CREATE TABLE IF NOT EXISTS ${configKeyspace}.${configTable} (name varchar PRIMARY KEY, contentType varchar, definition varchar)"

  setup()

  private[this] def setup(): Unit = {
    if (cassandraConnection.isReady) {
      createConfigurationTableIfNotExists()
      createConfiguredTablesIfNotExists()
    } else {
      system.scheduler.scheduleOnce(5 seconds) {
        setup()
      }
    }
  }

  def addTable(schema: TableSchema): Unit = {

    createConfigurationTableIfNotExists()
    createKeyspaceIfNotExists(schema.keyspace)

    try {
      createTable(schema)
      addTableToService(schema)
    } catch {
      case e: AlreadyExistsException ⇒ {
        e.printStackTrace
        log.error(e.getTable + " already exists", e)
      }
    }
  }

  def getTables: Map[String, TableSchema] = {

    createConfigurationTableIfNotExists()

    var tables: Map[String, TableSchema] = Map()

    val rs = executeCQL(s"SELECT * FROM ${configKeyspace}.${configTable}")
    if (rs != None) {
      val set = rs.get
      while (!set.isExhausted) {
        val json = set.one().getString("definition")
        val table = json.parseJson.convertTo[TableSchema]
        tables += (table.keyspace + table.table) → table
      }
    }

    tables
  }

  def insertRow(keyspace: String, table: String, data: String): Unit = {
    executeCQL(s"INSERT INTO ${filterName(keyspace)}.${filterName(table)} $data")
  }

  private[this] def createConfiguredTablesIfNotExists(): Unit = {
    if (config.tables.size > 0) {
      config.tables.foreach(json ⇒ {
        try {
          addTable(json.parseJson.convertTo[TableSchema])
        } catch {
          case unknown: Throwable ⇒ {
            log.error(unknown, "Unable to create the configured table: " + unknown.getMessage)
          }
        }
      })
    }
  }

  private[this] def createKeyspaceIfNotExists(keyspace: String): Unit = {
    executeCQL(s"CREATE KEYSPACE IF NOT EXISTS ${filterName(keyspace)} WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor':3}")
  }

  private[this] def createTable(schema: TableSchema): Unit = {
    val columns = schema.columns.foldLeft("")((b, a) ⇒ s"$b\n${a.name} ${if (a.`type`.isDefined) a.`type`.get else "text"},")
    val pKeys = schema.columns.filter(x ⇒ x.key.isDefined && x.key.get == KeyType.Partition).map(_.name).mkString("(", ", ", ")")
    if (pKeys.replaceAll("[(), ]", "").isEmpty) throw new RuntimeException("The partition key cannot be empty")

    val cKeys = schema.columns.filter(x ⇒ x.key.isDefined && x.key.get == KeyType.Clustering).map(_.name).mkString("", ", ", "")
    val primaryKey = if (cKeys.isEmpty) pKeys else pKeys + "," + cKeys

    executeCQL(
      s"""
      CREATE TABLE ${filterName(schema.keyspace)}.${filterName(schema.table)}
      (
          $columns
          PRIMARY KEY ($primaryKey)
      )
      """)
  }

  private def addTableToService(schema: TableSchema): Unit = {
    val definition = schema.toJson.compactPrint
    val contentType = if (schema.contentType.isDefined) schema.contentType.get else ""
    insertRow(
      configKeyspace,
      configTable,
      s"(name, contentType, definition) VALUES ('${schema.table}','${contentType}','${filterDefinition(definition)}')")
  }

  private def createConfigurationTableIfNotExists(): Unit = {
    createKeyspaceIfNotExists(configKeyspace)
    executeCQL(configTableSchema)
  }

  private def executeCQL(cql: String): Option[ResultSet] = {
    log.debug(cql)

    try {
      Option(cassandraConnection.getSession.execute(cql))
    } catch {
      case e: AlreadyExistsException ⇒ {
        log.error((if (e.getTable != null) "Table `" + e.getTable else "Keyspace `" + e.getKeyspace) + "` already exists", e)
        None
      }

      case e: SyntaxError ⇒ {
        log.error(s"The query syntax is not valid: `${cql}`: " + e.getMessage, e)
        throw e
      }

      case e: InvalidQueryException ⇒ {
        log.error(s"The query failed: `${cql}`: " + e.getMessage, e)
        throw e
      }

      case e: NoHostAvailableException ⇒ {
        cassandraConnection.reconnect()
        setup()
        throw e
      }

      case unknown: Throwable ⇒ {
        log.error(unknown, unknown.getMessage)
        throw unknown
      }
    }
  }

  private[this] def filterName(name: String): String = {
    val x: String = name.replaceAll("[^\\p{IsAlphabetic}^\\p{IsDigit}]", "_")
    if (x(0).isDigit) '_' + x else x
  }

  private def filterDefinition(definition: String): String = {
    definition.replace("'", "''")
  }
}
