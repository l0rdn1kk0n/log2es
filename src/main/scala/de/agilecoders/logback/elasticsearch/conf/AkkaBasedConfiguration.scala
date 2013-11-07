package de.agilecoders.logback.elasticsearch.conf

import akka.util.Timeout
import ch.qos.logback.classic.Level
import com.typesafe.config.{ConfigFactory, Config}
import de.agilecoders.logback.elasticsearch.mapper.LoggingEventToXContentMapper
import java.io.IOException
import scala.concurrent.duration._
import scala.io.{Source, BufferedSource}

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

    lazy val noOfWorkers: Int = c.getInt("configuration.noOfWorkers")

    lazy val flushInterval: Int = c.getInt("configuration.flushInterval")

    lazy val converterTimeout: Timeout = Timeout(c.getInt("configuration.converterTimeout") milliseconds)

    lazy val shutdownAwaitTimeout: Timeout = Timeout(c.getInt("configuration.shutdownAwaitTimeout") milliseconds)

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

    lazy val discoveryFrequency: Int = c.getInt("configuration.http.discoveryFrequency")

    lazy val defaultMaxTotalConnectionPerRoute: Int = c.getInt("configuration.http.defaultMaxTotalConnectionPerRoute")

    lazy val maxTotalConnection: Int = c.getInt("configuration.http.maxTotalConnection")

    lazy val discoveryEnabled: Boolean = c.getBoolean("configuration.http.discoveryEnabled")

    lazy val multiThreaded: Boolean = c.getBoolean("configuration.http.multiThreaded")

}
