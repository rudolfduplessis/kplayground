name := "kplayground"

version := "0.1"

scalaVersion := "2.12.7"

//https://github.com/sbt/sbt/issues/3618
val workaround: Unit = {
  sys.props += "packaging.type" -> "jar"
  ()
}

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.2.3"
libraryDependencies += "org.apache.kafka" %% "kafka-streams-scala" % "2.0.0"
libraryDependencies += "net.cakesolutions" %% "scala-kafka-client" % "2.0.0"
