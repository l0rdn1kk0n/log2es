package de.agilecoders.logger.log2es.log4j2

import java.io.Serializable

import de.agilecoders.logger.log2es.core.{AppenderProperties, LogEventStream}
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.{Filter, Layout, LogEvent}

/**
 * Created by miha on 24.07.14.
 */
abstract class AppenderBase(_name: String, filter: Filter, layout: Layout[_ <: Serializable]) extends AbstractAppender(_name, filter, layout) with AppenderProperties {
  private lazy val conf = toConfiguration
  private lazy val logEventStream = LogEventStream(conf, Log4j2EventMapper(conf))

  override def append(event: LogEvent): Unit = isStarted match {
    case true => logEventStream.enqueue(event)
    case _ => // don't log anything as long as appender isn't started
  }

  override def setStopped(): Unit = {
    super.setStopped()

    logEventStream.shutdown()
  }
}
