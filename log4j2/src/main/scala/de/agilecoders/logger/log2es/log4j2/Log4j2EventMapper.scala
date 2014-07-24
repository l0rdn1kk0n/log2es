package de.agilecoders.logger.log2es.log4j2

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.mapper.{EventMapper, Fields, MapperFunctionRegistry}
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.impl.ThrowableProxy

/**
 * log4j2 specific logging event mapper
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class Log4j2EventMapper(conf: Configuration) extends EventMapper[LogEvent] {

  /**
   * hook to register mapper functions
   *
   * @param registry the registry to register mappers at
   * @return given registry for chaining
   */
  override protected def registerMapper(registry: MapperFunctionRegistry[LogEvent]) = {
    registry
      .register(Fields.MESSAGE, event => nullToEmpty(event.getMessage.getFormattedMessage))
      .register(Fields.LEVEL, event => nullToEmpty(event.getLevel.name))
      .register(Fields.TIMESTAMP, event => nullToDefault(event.getTimeMillis, 0))
      .register(Fields.LOGGER, event => nullToNothing(event.getLoggerName))
      .register(Fields.MARKER, marker)
      .register(Fields.THREAD, event => event.getThreadName)
      .register(Fields.MDC, mdc)
      .register(Fields.STACKTRACE, stacktrace)
      .register(Fields.CALLER, event => nullToNothing(event.getLoggerFqcn))
  }

  private def mdc(event: LogEvent): Any = event.getContextMap match {
    case args: java.util.Map[String, String] => if (args.size() > 0) {
      args
    } else {
      nothing
    }
    case _ => nothing
  }

  private def marker(event: LogEvent): Any = event.getMarker match {
    case marker: Marker => nullToNothing(marker.getName)
    case _ => nothing
  }

  private def stacktrace(event: LogEvent): Any = event.getThrownProxy match {
    case proxy: ThrowableProxy => proxy.getExtendedStackTraceAsString
    case _ => nothing
  }
}
