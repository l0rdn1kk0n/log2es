package de.agilecoders.logback.elasticsearch.conf

import akka.util.Timeout
import ch.qos.logback.classic.Level
import com.typesafe.config.{ConfigFactory, Config}
import de.agilecoders.logback.elasticsearch.mapper.LoggingEventToXContentMapper
import java.io.IOException
import scala.concurrent.duration._
import scala.io.{Source, BufferedSource}

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

    def transformer: LoggingEventToXContentMapper

    def discoveryFrequency: Int

    def defaultMaxTotalConnectionPerRoute: Int

    def maxTotalConnection: Int

    def discoveryEnabled: Boolean

    def multiThreaded: Boolean
}

/**
 * main library configuration
 *
 * TODO miha: document
 *
 * @param c the baking `Config` instance
 */
case class AkkaBasedConfiguration(private val c: Config) extends Configuration {

    import scala.collection.convert.WrapAsScala._

    private[this] lazy val fields: java.util.List[String] = c.getStringList("configuration.fields")

    lazy val useAsyncHttp: Boolean = c.getBoolean("configuration.http.useAsyncHttp")

    lazy val retryCount: Int = c.getInt("configuration.retryCount")

    lazy val flushInterval: Int = c.getInt("configuration.flushInterval")

    lazy val converterTimeout: Timeout = Timeout(c.getInt("configuration.converterTimeout") milliseconds)

    lazy val shutdownAwaitTimeout: Timeout = Timeout(2 seconds)

    lazy val addCaller: Boolean = fields.contains("caller")

    lazy val addArguments: Boolean = fields.contains("arguments")

    lazy val addDate: Boolean = fields.contains("date")

    lazy val addStacktrace: Boolean = fields.contains("stacktrace")

    lazy val addMdc: Boolean = fields.contains("mdc")

    lazy val addMarker: Boolean = fields.contains("marker")

    lazy val addThread: Boolean = fields.contains("thread")

    lazy val addMessage: Boolean = fields.contains("message")

    lazy val addLogger: Boolean = fields.contains("logger")

    lazy val addTimestamp: Boolean = fields.contains("timestamp")

    lazy val addLevel: Boolean = fields.contains("level")

    lazy val initializeMapping: Boolean = c.getBoolean("configuration.initializeMapping")

    lazy val queueSize: Int = c.getInt("configuration.queueSize")

    lazy val indexName: String = c.getString("configuration.indexName")

    lazy val typeName: String = c.getString("configuration.typeName")

    lazy val discardable: String = c.getString("configuration.discardable")

    lazy val discardableLevel: Int = Level.valueOf(discardable).levelInt

    lazy val hosts: Iterable[String] = c.getStringList("configuration.hosts")

    lazy val transformer: LoggingEventToXContentMapper = LoggingEventToXContentMapper(this)

    lazy val discoveryFrequency: Int = c.getInt("configuration.http.discoveryFrequency")

    lazy val defaultMaxTotalConnectionPerRoute: Int = c.getInt("configuration.http.defaultMaxTotalConnectionPerRoute")

    lazy val maxTotalConnection: Int = c.getInt("configuration.http.maxTotalConnection")

    lazy val discoveryEnabled: Boolean = c.getBoolean("configuration.http.discoveryEnabled")

    lazy val multiThreaded: Boolean = c.getBoolean("configuration.http.multiThreaded")

}
