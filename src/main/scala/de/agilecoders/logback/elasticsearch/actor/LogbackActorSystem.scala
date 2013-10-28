package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{PoisonPill, ActorSystem}
import akka.routing.Broadcast
import akka.util.Timeout
import ch.qos.logback.classic.Level
import com.typesafe.config.{ConfigFactory, Config}
import de.agilecoders.logback.elasticsearch.{LogbackContext, ILoggingEventToMapTransformer, DefaultLoggingEventToMapConverter}
import java.io.IOException
import scala.concurrent.duration._
import scala.io.{BufferedSource, Source}
import com.twitter.ostrich.stats.Stats

/**
 * Holds the `ActorSystem` instance.
 *
 * @author miha
 */
object LogbackActorSystem {
    private[this] lazy val initializer: Initializer = new Initializer

    lazy val configuration: Configuration = initializer.configuration
    lazy val instance: ActorSystem = initializer.system

    /**
     * shutdown actor system
     */
    def shutdown(): Unit = {
        instance.eventStream.publish(PoisonPill.getInstance)

        instance.shutdown()
        instance.awaitTermination()

        Console.out.append(Stats.get().toString)
    }

    private class Initializer {
        private[this] val name: String = "log2es"
        private[this] val config = ConfigFactory.load()

        private[this] val _configuration = (findCustomConfig match {
            case Some(s: Source) => {
                ConfigFactory.parseReader(s.bufferedReader()).withFallback(config)
            }
            case _ => config
        }).getConfig(name)

        val system = ActorSystem.create(name, _configuration)
        val configuration = Configuration(_configuration)

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

}

/**
 * main library configuration
 *
 * @param c the baking `Config` instance
 */
case class Configuration(private val c: Config) {

    import scala.collection.convert.WrapAsScala._

    private[this] lazy val fields: java.util.List[String] = c.getStringList("configuration.fields")

    lazy val useAsyncHttp: Boolean = c.getBoolean("configuration.http.useAsyncHttp")

    lazy val retryCount: Int = c.getInt("configuration.retryCount")

    lazy val flushInterval: Int = c.getInt("configuration.flushInterval")

    lazy val converterTimeout: Timeout = Timeout(c.getInt("configuration.converterTimeout") milliseconds)

    lazy val addCaller = fields.contains("caller")

    lazy val addArguments = fields.contains("arguments")

    lazy val addDate = fields.contains("date")

    lazy val addStacktrace = fields.contains("stacktrace")

    lazy val addMdc = fields.contains("mdc")

    lazy val addThread = fields.contains("thread")

    lazy val addMessage = fields.contains("message")

    lazy val addLogger = fields.contains("logger")

    lazy val addTimestamp = fields.contains("timestamp")

    lazy val addLevel = fields.contains("level")

    lazy val initializeMapping: Boolean = c.getBoolean("configuration.initializeMapping")

    lazy val queueSize: Int = c.getInt("configuration.queueSize")

    lazy val indexName: String = c.getString("configuration.indexName")

    lazy val typeName: String = c.getString("configuration.typeName")

    lazy val discardable: String = c.getString("configuration.discardable")

    lazy val discardableLevel: Int = Level.valueOf(discardable).levelInt

    lazy val hosts: Iterable[String] = c.getStringList("configuration.hosts")

    lazy val transformer: ILoggingEventToMapTransformer = DefaultLoggingEventToMapConverter(this)

    lazy val discoveryFrequency: Int = c.getInt("configuration.http.discoveryFrequency")
    lazy val defaultMaxTotalConnectionPerRoute: Int = c.getInt("configuration.http.defaultMaxTotalConnectionPerRoute")
    lazy val maxTotalConnection: Int = c.getInt("configuration.http.maxTotalConnection")
    lazy val discoveryEnabled: Boolean = c.getBoolean("configuration.http.discoveryEnabled")
    lazy val multiThreaded: Boolean = c.getBoolean("configuration.http.multiThreaded")

}
