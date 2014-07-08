package de.agilecoders.logger.log2es.core.common

/**
 * An EventParseException will be thrown if a log event can't be parsed. This exception doesn't contain a stacktrace.
 *
 * @author miha
 */
class EventParseException(private val cause: Throwable) extends RuntimeExceptionWithoutStack(cause)
