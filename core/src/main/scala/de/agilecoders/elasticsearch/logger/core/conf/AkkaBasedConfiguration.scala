package de.agilecoders.elasticsearch.logger.core.conf

import akka.util.Timeout
import ch.qos.logback.classic.Level
import com.typesafe.config.{ConfigFactory, Config}
import java.io.IOException
import scala.concurrent.duration._
import scala.io.{BufferedSource, Source}

/**
 * Akka based implementation of `Configuration` trait
 *
 * @param name the unique log2es name
 */
case class AkkaBasedConfiguration() extends Configuration {

    import scala.collection.convert.WrapAsScala._

    /**
     * initializes the log2es configuration.
     */
    private[this] def initializeConfigInstance(): Config = {
        val config = ConfigFactory.load()

        (findCustomConfig match {
            case Some(s: Source) => ConfigFactory.parseReader(s.bufferedReader()).withFallback(config)
            case _ => config
        }).getConfig(name)
    }

    /**
     * try to find a custom configuration file in classpath
     *
     * @return buffered source of custom configuration file
     */
    private[this] def findCustomConfig: Option[BufferedSource] = {
        toPath(s"/$name.conf") match {
            case None => toPath(s"$name.conf")
            case some@Some(source: BufferedSource) => some
            case _ => throw new IllegalArgumentException("can't find custom config") // this should never ever happen
        }
    }

    /**
     * transforms a given path to a config file into a buffered source
     *
     * @param pathToConfig the full path to config file
     * @return buffered source of given path
     */
    private[this] def toPath(pathToConfig: String): Option[BufferedSource] = try {
        Some(Source.fromURL(getClass.getResource(pathToConfig)))
    } catch {
        case e: IOException => None
    }

    private[this] lazy val fields: java.util.List[String] = file.getStringList("configuration.fields")
    
    lazy val file: Config = initializeConfigInstance()

    lazy val useAsyncHttp: Boolean = file.getBoolean("configuration.http.useAsyncHttp")

    lazy val retryCount: Int = file.getInt("configuration.retryCount")

    lazy val noOfWorkers: Int = file.getInt("configuration.noOfWorkers")

    lazy val flushInterval: Int = file.getInt("configuration.flushInterval")

    lazy val converterTimeout: Timeout = Timeout(file.getInt("configuration.converterTimeout") milliseconds)

    lazy val shutdownAwaitTimeout: Timeout = Timeout(file.getInt("configuration.shutdownAwaitTimeout") milliseconds)

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

    lazy val initializeMapping: Boolean = file.getBoolean("configuration.initializeMapping")

    lazy val queueSize: Int = file.getInt("configuration.queueSize")

    lazy val indexName: String = file.getString("configuration.indexName")

    lazy val typeName: String = file.getString("configuration.typeName")

    lazy val discardable: String = file.getString("configuration.discardable")

    lazy val hosts: Iterable[String] = file.getStringList("configuration.hosts")

    lazy val sniffHostnames: Boolean = file.getBoolean("configuration.sniffHostnames")

    lazy val clusterName: String = file.getString("configuration.clusterName")

}
