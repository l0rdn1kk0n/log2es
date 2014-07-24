package de.agilecoders.logger.log2es.log4j

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.mapper.{EventMapper, Fields, MapperFunctionRegistry}
import org.apache.log4j.spi.{ThrowableInformation, LoggingEvent}

/**
 * log4j specific logging event mapper
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class Log4jEventMapper(conf: Configuration) extends EventMapper[LoggingEvent] {

  /**
   * hook to register mapper functions
   *
   * @param registry the registry to register mappers at
   * @return given registry for chaining
   */
  override protected def registerMapper(registry: MapperFunctionRegistry[LoggingEvent]) = {
    registry
      .register(Fields.MESSAGE, event => nullToEmpty(event.getRenderedMessage))
      .register(Fields.LEVEL, event => nullToEmpty(event.getLevel.toString))
      .register(Fields.TIMESTAMP, event => nullToDefault(event.getTimeStamp, 0))
      .register(Fields.LOGGER, event => nullToNothing(event.getLoggerName))
      .register(Fields.THREAD, event => event.getThreadName)
      .register(Fields.MDC, mdc)
      .register(Fields.STACKTRACE, stacktrace)
      .register(Fields.CALLER, event => nullToNothing(event.getFQNOfLoggerClass))
  }

  private def mdc(event: LoggingEvent): Any = event.getProperties match {
    case args: java.util.Map[_,_] => if (args.size() > 0) {
      args
    } else {
      nothing
    }
    case _ => nothing
  }

  private def stacktrace(event: LoggingEvent): Any = event.getThrowableInformation match {
    case proxy: ThrowableInformation => proxy.getThrowableStrRep.mkString("\n")
    case _ => nothing
  }
}
