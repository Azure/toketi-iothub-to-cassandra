logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

// sbt assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

// Docker (Note: in case of problems, try disabling sbt assembly)
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.2.0-M7")
