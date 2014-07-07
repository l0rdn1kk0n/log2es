package de.agilecoders.logger.log2es.core

import akka.util.Timeout

import scala.concurrent.duration._

/**
 * log2es configuration
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class Configuration(defaultTimeout: Timeout = Timeout(3.seconds),
                         incomingBufferSize: Int = 5000,
                         outgoingBulkSize: Int = 1000,
                         clientType: String = "http",
                         indexName: String = "log2es",
                         hostName: String = "",
                         serviceName: String = "",
                         typeName: String = "logline",
                         userAgent: String = "log2es",
                         clusterName: String = "elasticsearch",
                         fields: Seq[String] = Seq("message", "stacktrace", "thread", "timestamp", "logger"),
                         flushQueueTime: Duration = 5.seconds,
                         actorSystemName: String = "log2es") {


  def isMessageEnabled = fields.contains("message")

  def isStacktraceEnabled = fields.contains("stacktrace")

  def isThreadEnabled = fields.contains("thread")

  def isMDCEnabled = fields.contains("mdc")

  def isTimestampEnabled = fields.contains("timestamp")

  def dynamicTypeName = typeName

}
