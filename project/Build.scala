import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin._

object BuildSettings {
  val customBuildSettings = Seq(
    organization := "de.agilecoders.logger",
    version := "0.1.2",
    scalaVersion := "2.11.1",
    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    javacOptions ++= Seq("-source", jdkVersion, "-target", jdkVersion),
    scalacOptions ++= Seq("-target:jvm-" + jdkVersion)
  )
  private val jdkVersion = "1.7"
}

object PublishSettings {
  val customPublishSettings = Seq(
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false},
    publishMavenStyle := true,
    pomExtra := <url>https://github.com/l0rdn1kk0n/log2es</url>
      <scm>
        <url>git@github.com:l0rdn1kk0n/log2es.git</url>
        <connection>scm:git:git@github.com:l0rdn1kk0n/log2es.git</connection>
        <developerConnection>scm:git:git@github.com:l0rdn1kk0n/log2es.git</developerConnection>
      </scm>
      <licenses>
        <license>
          <name>The Apache Software License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <developers>
        <developer>
          <id>miha</id>
          <name>Michael Haitz</name>
          <email>michael.haitz@agilecoders.de</email>
          <organization>agilecoders.de</organization>
          <roles>
            <role>Owner</role>
            <role>Comitter</role>
          </roles>
        </developer>
      </developers>
  )

}

object Dependencies {

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka withSources()
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka withSources()
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test" withSources()
  val akkaStreeam = "com.typesafe.akka" %% "akka-stream-experimental" % Version.akkaStream withSources()
  val junit = "junit" % "junit" % Version.junit % "test" withSources()
  val logback = "ch.qos.logback" % "logback-classic" % Version.logback withSources()
  val dispatch = "net.databinder.dispatch" %% "dispatch-core" % Version.dispatch withSources()
  val json4s = "org.json4s" %% "json4s-jackson" % Version.json4s withSources()
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test" withSources()
  val asyncHttpClient = "com.ning" % "async-http-client" % Version.asyncHttpClient withSources()
  val log4j2 = "org.apache.logging.log4j" % "log4j-core" % Version.log4j2 withSources()
  val log4j2Api = "org.apache.logging.log4j" % "log4j-api" % Version.log4j2 withSources()
  val log4j = "log4j" % "log4j" % Version.log4j withSources()
  val testDependencies = Seq(akkaTestKit, junit, scalaTest)
  val coreDependencies = Seq(akkaActor, akkaSlf4j, akkaStreeam, dispatch, asyncHttpClient, json4s)
  val logbackDependencies = Seq(logback, json4s)
  val log4j2Dependencies = Seq(log4j2, log4j2Api, json4s)
  val log4jDependencies = Seq(log4j, json4s)

  private object Version {
    val akka = "2.3.4"
    val akkaStream = "0.4"
    val junit = "4.5"
    val logback = "1.0.13"
    val dispatch = "0.11.1"
    val json4s = "3.2.9"
    val scalaTest = "2.2.0"
    val asyncHttpClient = "1.7.4"
    val log4j2 = "2.0"
    val log4j = "1.2.17"
  }

}

object Log2esBuild extends Build {

  import BuildSettings._
  import Dependencies._
  import PublishSettings._
  import sbt.Defaults._

  lazy val root = Project(id = "log2es", base = file("."), settings = customSettings ++ Seq(
    publishLocal := {},
    publish := {}
  )).aggregate(core, logback, log4j)
  lazy val core = Project(id = "log2es-core", base = file("./core"), settings = customSettings ++ Seq(
    name := "log2es-core",
    libraryDependencies ++= coreDependencies ++ testDependencies
  ))
  lazy val logback = Project(id = "log2es-logback", base = file("./logback"), settings = customSettings ++ Seq(
    name := "log2es-logback",
    libraryDependencies ++= logbackDependencies ++ testDependencies
  )).dependsOn(core)
  lazy val log4j2 = Project(id = "log2es-log4j2", base = file("./log4j2"), settings = customSettings ++ Seq(
    name := "log2es-log4j2",
    libraryDependencies ++= log4j2Dependencies ++ testDependencies
  )).dependsOn(core)
  lazy val log4j = Project(id = "log2es-log4j", base = file("./log4j"), settings = customSettings ++ Seq(
    name := "log2es-log4j",
    libraryDependencies ++= log4jDependencies ++ testDependencies
  )).dependsOn(core)
  lazy val jul = Project(id = "log2es-jul", base = file("./jul"), settings = customSettings ++ Seq(
    name := "log2es-jul",
    libraryDependencies ++= log4jDependencies ++ testDependencies
  )).dependsOn(core)

  private lazy val customSettings = super.settings ++ defaultSettings ++ releaseSettings ++ customBuildSettings ++ customPublishSettings ++ Seq(
    // custom settings...
  )
}