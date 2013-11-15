package de.agilecoders.elasticsearch.logger.core.conf

import akka.util.Timeout
import com.typesafe.config.Config

/**
 * Configuration companion object that is responsible to initialize configuration.
 */
object Configuration {
    val name: String = "log2es"

    lazy val instance = AkkaBasedConfiguration()
}

/**
 * base configuration trait
 */
trait Configuration {

    def name: String = Configuration.name

    def file: Config

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

    def hosts: Iterable[String]

    def sniffHostnames: Boolean

    def clusterName: String

    def ttl: Long

}