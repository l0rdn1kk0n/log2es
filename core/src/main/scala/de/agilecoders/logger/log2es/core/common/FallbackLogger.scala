package de.agilecoders.logger.log2es.core.common

import org.slf4j.helpers.MessageFormatter

/**
 * fallback logger if access to elasticsearch isn't possible
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait FallbackLogger {
  //private lazy val logger = LoggerFactory.getLogger("log2es-fallback-logger")

  def info(message: String, args: AnyRef*): Unit = Console.println(MessageFormatter.arrayFormat(message, args.toArray))

  def warn(message: String, args: AnyRef*): Unit = Console.println(MessageFormatter.arrayFormat(message, args.toArray))

  def error(message: String, args: AnyRef*): Unit = Console.err.println(MessageFormatter.arrayFormat(message, args.toArray))

  def error(message: String, throwable: Throwable): Unit = Console.err.println(MessageFormatter.format(message, throwable))

  def debug(message: String, args: AnyRef*): Unit = Console.println(MessageFormatter.arrayFormat(message, args.toArray))

}

/**
 * internal logger for log2es (uses fallback logger)
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object InternalLogger extends FallbackLogger