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
                                     addCaller: Boolean = true,
                                     addArguments: Boolean = true,
                                     addDate: Boolean = true,
                                     addStacktrace: Boolean = true,
                                     addMdc: Boolean = true,
                                     addMarker: Boolean = true,
                                     addMessage: Boolean = true,
                                     addThread: Boolean = true,
                                     addLogger: Boolean = true,
                                     addTimestamp: Boolean = true,
                                     addLevel: Boolean = true,
                                     queueSize: Int = 100,
                                     indexName: String = "log",
                                     typeName: String = "logline",
                                     discardable: String = "INFO",
                                     hosts: Iterable[String] = Seq("127.0.0.1:9300"),
                                     sniffHostnames: Boolean = true,
                                     initializeMapping: Boolean = false,
                                     file: Config = null,
                                     retryCount: Int = 3) extends Configuration {}
