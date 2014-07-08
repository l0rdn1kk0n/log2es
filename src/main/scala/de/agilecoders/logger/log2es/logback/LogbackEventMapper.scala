package de.agilecoders.logger.log2es.logback

import ch.qos.logback.classic.spi.{ILoggingEvent, ThrowableProxy, ThrowableProxyUtil}
import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.mapper.{EventMapper, Fields, MapperFunctionRegistry}
import org.slf4j.Marker

/**
 * logback specific logging event mapper
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class LogbackEventMapper(conf: Configuration) extends EventMapper[ILoggingEvent] {

  /**
   * hook to register mapper functions
   *
   * TODO miha: add only configured fields
   *
   * @param registry the registry to register mappers at
   * @return given registry for chaining
   */
  override protected def registerMapper(registry: MapperFunctionRegistry[ILoggingEvent]) = {
    registry
      .register(Fields.MESSAGE, event => nullToEmpty(event.getFormattedMessage))
      .register(Fields.LEVEL, event => nullToEmpty(event.getLevel.levelStr))
      .register(Fields.TIMESTAMP, event => nullToDefault(event.getTimeStamp, 0))
      .register(Fields.LOGGER, event => nullToNothing(event.getLoggerName))
      .register(Fields.MARKER, marker)
      .register(Fields.THREAD, threadName)
      .register(Fields.MDC, mdc)
      .register(Fields.ARGUMENTS, arguments)
      .register(Fields.STACKTRACE, stacktrace)
  }

  private def arguments(event: ILoggingEvent): Any = event.getArgumentArray match {
    case args: Array[Object] => if (args.length > 0) {
      args.map(_.toString)
    } else {
      nothing
    }
    case _ => nothing
  }

  private def mdc(event: ILoggingEvent): Any = event.getMDCPropertyMap match {
    case args: java.util.Map[String, String] => if (args.size() > 0) {
      args
    } else {
      nothing
    }
    case _ => nothing
  }

  private def threadName(event: ILoggingEvent): Any = event.getThreadName match {
    case name: String => name
    case _ => nothing
  }

  private def marker(event: ILoggingEvent): Any = event.getMarker match {
    case marker: Marker => nullToNothing(marker.getName)
    case _ => nothing
  }

  private def stacktrace(event: ILoggingEvent): Any = event.getThrowableProxy match {
    case proxy: ThrowableProxy => ThrowableProxyUtil.asString(proxy)
    case _ => nothing
  }
}
