// Copyright (c) Microsoft. All rights reserved.

name := "IoTHub-to-Cassandra"
organization := "com.microsoft.azure.iot"
version := "0.6.1"

scalaVersion := "2.12.1"

resolvers += "iothub-react snapshots" at "https://dl.bintray.com/microsoftazuretoketi/toketi-repo"
//resolvers += "iothub-react staging" at "https://oss.sonatype.org/service/local/repositories/commicrosoftazure-1516/content"

libraryDependencies ++= {
  val akkaStreamVersion = "2.4.16"
  val akkaHttpVersion = "10.0.1"
  val iothubreactVersion = "0.8.0"
  val cassandraDriverVersion = "3.1.3"
  val jodaTimeVersion = "2.9.7"
  val iodaConvertVersion = "1.8.1"
  val scalaTestVersion = "3.0.1"

  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.microsoft.azure.iot" % "iothub-react_2.12" % iothubreactVersion,
    "com.datastax.cassandra" % "cassandra-driver-core" % cassandraDriverVersion,
    "joda-time" % "joda-time" % jodaTimeVersion,
    "org.joda" % "joda-convert" % iodaConvertVersion,

    "org.scalatest" %% "scalatest" % scalaTestVersion % "it,test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "it,test"
  )
}

lazy val root = project.in(file(".")).configs(IntegrationTest)
Defaults.itSettings

/* Publishing options
 * see http://www.scala-sbt.org/0.13/docs/Artifacts.html
 */
publishArtifact in Test := true
publishArtifact in(Compile, packageDoc) := true
publishArtifact in(Compile, packageSrc) := true
publishArtifact in(Compile, packageBin) := true

// Note: for Bintray, unpublish using SBT
licenses += ("MIT", url("https://github.com/Azure/toketi-iothub-to-cassandra/blob/master/LICENSE"))
publishMavenStyle := true

// Required in Sonatype
pomExtra :=
  <url>https://github.com/Azure/toketi-iothub-to-cassandra</url>
    <scm>
      <url>https://github.com/Azure/toketi-iothub-to-cassandra</url>
    </scm>
    <developers>
      <developer>
        <id>microsoft</id> <name>Microsoft</name>
      </developer>
    </developers>

// Docker
enablePlugins(JavaAppPackaging)
maintainer in Docker := "Devis Lucato <devis@microsoft.com>"
dockerBaseImage := "toketi/openjdk-8-jre-alpine-bash"
dockerExposedPorts := Seq(9000)
dockerRepository := Some("toketi")
dockerUpdateLatest := true
dockerBuildOptions ++= Seq("--squash", "--compress", "--label", "Tags=toketi,dluc")

// Miscs
logLevel := Level.Debug // Debug|Info|Warn|Error
scalacOptions ++= Seq("-deprecation", "-explaintypes", "-unchecked", "-feature")
showTiming := true
fork := true
parallelExecution := true
