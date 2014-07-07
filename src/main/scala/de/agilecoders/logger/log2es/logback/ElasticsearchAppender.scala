package de.agilecoders.logger.log2es.logback

import ch.qos.logback.classic.spi.{ILoggingEvent, LoggingEvent}
import ch.qos.logback.core.UnsynchronizedAppenderBase
import de.agilecoders.logger.log2es.core.{AppenderProperties, ILogEventMapper, LogEventStream}

/**
 * elasticsearch asynchronous and remote logback appender
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class ElasticsearchAppender() extends UnsynchronizedAppenderBase[ILoggingEvent] with AppenderProperties {

  private lazy val mapper = new ILogEventMapper[ILoggingEvent] {
    override def map(event: ILoggingEvent): String = {
      s"""{"message":"${event.getFormattedMessage}"}"""
    }
  }

  private lazy val conf = toConfiguration

  private lazy val logEventStream = LogEventStream(conf, mapper)

  /**
   * start elasticsearch logger
   */
  override def start() = {
    super.start()
  }

  /**
   * shutdown elasticsearch logger
   */
  override def stop() = {
    super.stop()

    logEventStream.shutdown()
  }


  /**
   * append log event to elasticsearch
   *
   * @param eventObject the log event
   */
  override def append(eventObject: ILoggingEvent) = {
    // necessary because of async call
    eventObject match {
      case e: LoggingEvent =>
        e.prepareForDeferredProcessing()

      case e: ILoggingEvent =>
        if (conf.isMessageEnabled) {
          eventObject.getFormattedMessage
        }

        if (conf.isThreadEnabled) {
          eventObject.getThreadName
        }

        if (conf.isMDCEnabled) {
          eventObject.getMDCPropertyMap
        }

      case _ =>
        throw new IllegalArgumentException("invalid logging event: " + eventObject.toString)
    }

    logEventStream.enqueue(eventObject)
  }

}
