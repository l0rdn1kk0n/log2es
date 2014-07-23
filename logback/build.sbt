name := """log2es-logback"""

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.5" % "test" withSources(),
  "ch.qos.logback" % "logback-classic" % "1.0.13" withSources(),
  "org.json4s" %% "json4s-jackson" % "3.2.9" withSources()
)