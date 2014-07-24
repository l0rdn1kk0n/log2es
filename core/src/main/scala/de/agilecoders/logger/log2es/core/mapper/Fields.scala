package de.agilecoders.logger.log2es.core.mapper

/**
 * All available log event fields. Please update the elasticsearch mapping if there's a new field.
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object Fields {

  val MESSAGE = "message"
  val THREAD = "thread"
  val LEVEL = "level"
  val ARGUMENTS = "arguments"
  val LOGGER = "logger"
  val MARKER = "marker"
  val MDC = "mdc"
  val TIMESTAMP = "timestamp"
  val STACKTRACE = "stacktrace"
  val CALLER = "caller"
  val SERVICE = "service"
  val HOSTNAME = "hostname"

  private lazy val fields = Seq(
    MESSAGE, THREAD, LEVEL,
    ARGUMENTS, LOGGER, MARKER,
    MDC, TIMESTAMP, STACKTRACE,
    CALLER, SERVICE, HOSTNAME
  )

  /**
   * @return all available fields
   */
  def all(): Seq[String] = fields

  /**
   * parse a fields string to a sequence of fields.
   *
   * @param fieldListAsString the list to parse
   * @return parsed and filtered list
   */
  def parse(fieldListAsString: String): Seq[String] = fieldListAsString.split(',').map(_.trim.toLowerCase).filter(fields.contains(_)).toSeq

}
