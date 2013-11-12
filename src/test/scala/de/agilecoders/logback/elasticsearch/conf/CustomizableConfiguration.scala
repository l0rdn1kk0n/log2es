package de.agilecoders.logback.elasticsearch.conf


import akka.util.Timeout
import scala.concurrent.duration._
import de.agilecoders.logback.elasticsearch.mapper.LoggingEventToXContentMapper
import ch.qos.logback.classic.Level
import com.typesafe.config.Config

/**
 * custom configuration for tests
 *
 * @author miha
 */
case class CustomizableConfiguration(   flushInterval: Int = 3000,
                                        useAsyncHttp: Boolean = false,
                                        converterTimeout:Timeout = 3 seconds,
                                        shutdownAwaitTimeout:Timeout = 3 seconds,
                                        clusterName: String = "elasticsearch",
                                        noOfWorkers:Int = 5,
                                        addCaller:Boolean = true,
                                        addArguments:Boolean = true,
                                        addDate:Boolean = true,
                                        addStacktrace:Boolean = true,
                                        addMdc:Boolean = true,
                                        addMarker:Boolean = true,
                                        addMessage:Boolean = true,
                                        addThread:Boolean = true,
                                        addLogger:Boolean = true,
                                        addTimestamp:Boolean = true,
                                        addLevel:Boolean = true,
                                        sniffHostnames: Boolean = true,
                                        initializeMapping:Boolean = false,
                                        retryCount: Int = 3) extends Configuration {


    def queueSize = 100

    def indexName = "log"

    def typeName = "logline"

    def discardable = Level.INFO.levelStr

    def discardableLevel = Level.INFO_INT

    def hosts = Seq("127.0.0.1:9300")

    def transformer = LoggingEventToXContentMapper(this)

    def file: Config = null
}
