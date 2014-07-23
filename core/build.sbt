name := """log2es-core"""

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.4" withSources(),
  "com.typesafe.akka" % "akka-slf4j_2.10" % "2.3.4" withSources(),
  "com.typesafe.akka" % "akka-testkit_2.10" % "2.3.4" % "test" withSources(),
  "com.typesafe.akka" % "akka-stream-experimental_2.10" % "0.4" withSources(),
  "junit" % "junit" % "4.5" % "test" withSources(),
  "ch.qos.logback" % "logback-classic" % "1.0.13" withSources(),
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.1" withSources(),
  "org.json4s" %% "json4s-jackson" % "3.2.9" withSources(),
  //"org.scalatest"     % "scalatest"                        % "1.6.1" % "test" withSources,
  "com.ning" % "async-http-client" % "1.7.4" withSources()
)