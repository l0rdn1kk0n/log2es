package de.agilecoders.logger.log2es.log4j

import de.agilecoders.logger.log2es.core.{AppenderProperties, LogEventStream}
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

/**
 * Created by miha on 24.07.14.
 */
case class ElasticsearchAppender() extends AppenderSkeleton with AppenderProperties {

  private lazy val conf = toConfiguration
  private lazy val logEventStream = LogEventStream(conf, Log4jEventMapper(conf))

  override def append(event: LoggingEvent): Unit = {
    if (!logEventStream.isRunning) {
      logEventStream.start()
    }

    if (conf.isMDCEnabled) {
      event.getMDCCopy()
    }
    if (conf.isThreadEnabled) {
      event.getThreadName
    }

    logEventStream.enqueue(event)
  }

  override def requiresLayout(): Boolean = false

  override def close(): Unit = logEventStream.shutdown()

}
