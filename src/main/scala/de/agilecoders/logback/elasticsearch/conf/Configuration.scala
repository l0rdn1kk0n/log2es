package de.agilecoders.logback.elasticsearch.conf

import com.typesafe.config.{ConfigFactory, Config}
import java.io.IOException
import scala.io.{BufferedSource, Source}
import akka.util.Timeout
import de.agilecoders.logback.elasticsearch.mapper.LoggingEventToXContentMapper

object Configuration {
    val name: String = "log2es"

    lazy val configInstance = initializeConfigInstance()
    lazy val instance = AkkaBasedConfiguration(configInstance)

    private[this] def initializeConfigInstance(): Config = {
        val config = ConfigFactory.load()

        (findCustomConfig match {
            case Some(s: Source) => {
                ConfigFactory.parseReader(s.bufferedReader()).withFallback(config)
            }
            case _ => config
        }).getConfig(name)
    }

    private[this] def findCustomConfig: Option[BufferedSource] = {
        toPath(s"/$name.conf") match {
            case None => toPath(s"$name.conf")
            case some@Some(source: BufferedSource) => some
            case _ => throw new IllegalArgumentException("can't find custom config")
        }
    }

    private[this] def toPath(pathToConfig: String): Option[BufferedSource] = try {
        Some(Source.fromURL(getClass.getResource(pathToConfig)))
    } catch {
        case e: IOException => None
    }
}


trait Configuration {

    def useAsyncHttp: Boolean

    def retryCount: Int

    def noOfWorkers: Int

    def flushInterval: Int

    def converterTimeout: Timeout

    def shutdownAwaitTimeout: Timeout

    def addCaller: Boolean

    def addArguments: Boolean

    def addDate: Boolean

    def addStacktrace: Boolean

    def addMdc: Boolean

    def addMarker: Boolean

    def addThread: Boolean

    def addMessage: Boolean

    def addLogger: Boolean

    def addTimestamp: Boolean

    def addLevel: Boolean

    def initializeMapping: Boolean

    def queueSize: Int

    def indexName: String

    def typeName: String

    def discardable: String

    def discardableLevel: Int

    def hosts: Iterable[String]

    def discoveryFrequency: Int

    def defaultMaxTotalConnectionPerRoute: Int

    def maxTotalConnection: Int

    def discoveryEnabled: Boolean

    def multiThreaded: Boolean
}