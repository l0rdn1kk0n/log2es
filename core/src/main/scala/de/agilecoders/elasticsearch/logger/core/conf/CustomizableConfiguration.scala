package de.agilecoders.elasticsearch.logger.core.conf

import akka.util.Timeout
import com.typesafe.config.Config
import scala.concurrent.duration._

/**
 * custom configuration for tests
 *
 * @author miha
 */
case class CustomizableConfiguration(flushInterval: Int = 3000,
                                     useAsyncHttp: Boolean = false,
                                     converterTimeout: Timeout = 3 seconds,
                                     shutdownAwaitTimeout: Timeout = 3 seconds,
                                     clusterName: String = "elasticsearch",
                                     noOfWorkers: Int = 5,
                                     ttl: Long = 1,
                                     queueSize: Int = 100,
                                     indexName: String = "log",
                                     typeName: String = "logline",
                                     discardable: String = "INFO",
                                     hosts: Iterable[String] = Seq("127.0.0.1:9300"),
                                     sniffHostnames: Boolean = true,
                                     initializeMapping: Boolean = false,
                                     file: Config = null,
                                     retryCount: Int = 3) extends Configuration {

    var addCaller: Boolean = true
    var addArguments: Boolean = true
    var addDate: Boolean = true
    var addStacktrace: Boolean = true
    var addMdc: Boolean = true
    var addMarker: Boolean = true
    var addMessage: Boolean = true
    var addThread: Boolean = true
    var addLogger: Boolean = true
    var addTimestamp: Boolean = true
    var addLevel: Boolean = true

}
