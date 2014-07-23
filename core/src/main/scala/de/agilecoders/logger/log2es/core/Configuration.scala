package de.agilecoders.logger.log2es.core

import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicReference

import akka.util.Timeout
import de.agilecoders.logger.log2es.core.common.RuntimeExceptionWithoutStack
import de.agilecoders.logger.log2es.core.mapper.Fields

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.io.Source

/**
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object Configuration {

  /**
   * TODO miha
   *
   * @param mappingFile
   * @param typeName
   * @param ttl
   * @return
   */
  def loadMapping(mappingFile: String, typeName: String, ttl: String): String = load(mappingFile, v => {
    v.replace("%{typeName}", typeName).replace("%{ttl}", ttl)
  })

  /**
   * TODO miha
   *
   * @param indexFile
   * @return
   */
  def loadIndexDefinition(indexFile: String): String = load(indexFile, v => {
    v
  })

  private def load(file: String, callback: String => String): String = {
    Thread.currentThread().getContextClassLoader.getResource(file) match {
      case url: URL =>
        var source: Source = null
        try {
          source = Source.fromURL(url)
          callback(source.getLines().mkString)
        } finally {
          if (source != null) {
            source.close()
          }
        }
      case _ => throw new RuntimeExceptionWithoutStack(s"can't find resource: $file")
    }
  }

}

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
                         name: String = "log2es",
                         host: String = "http://localhost:9200",
                         hostName: String = "",
                         serviceName: String = "",
                         typeName: String = "logline",
                         userAgent: String = "log2es",
                         updateMapping: Boolean = true,
                         gzip: Boolean = false,
                         ttl: String = "90d",
                         esConfigurationFile: String = "log2es/es_configuration.json",
                         clusterName: String = "elasticsearch",
                         fields: Seq[String] = Seq(Fields.MESSAGE, Fields.STACKTRACE, Fields.THREAD, Fields.TIMESTAMP, Fields.LOGGER, Fields.LEVEL),
                         flushQueueTime: Duration = 5.seconds,
                         actorSystemName: String = "log2es",
                         typeNameUpdateInterval: Duration = Duration.Zero) {

  private lazy val listener = ArrayBuffer[String => Unit]()

  def onTypeNameChanged(f: String => Unit) = listener += f

  private lazy val dynamicTypeNameHolder = new AtomicReference[String](typeName)
  private lazy val pattern = typeName.replaceFirst(".*%\\{([^\\}]*)\\}.*", "$1")

  def updateTypeName(): Unit = if (typeName.contains("%") && pattern.length > 0) {
    val dateFormat = new SimpleDateFormat(pattern)
    val current = dynamicTypeNameHolder.get()
    val newValue = typeName.replace("%{" + pattern + "}", dateFormat.format(new Date()))

    if (current != newValue) {
      listener.foreach(_(newValue))

      dynamicTypeNameHolder.compareAndSet(current, newValue)
    }
  }

  def isMessageEnabled = fields.contains("message")

  def isStacktraceEnabled = fields.contains("stacktrace")

  def isThreadEnabled = fields.contains("thread")

  def isMDCEnabled = fields.contains("mdc")

  def isTimestampEnabled = fields.contains("timestamp")

  def dynamicTypeName = dynamicTypeNameHolder.get()

  updateTypeName()

}
