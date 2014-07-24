package de.agilecoders.logger.log2es.core.mapper

import de.agilecoders.logger.log2es.core.Configuration
import org.json4s.Extraction._
import org.json4s.JsonAST.JNothing
import org.json4s.jackson.JsonMethods.compact
import org.json4s.{DefaultFormats, JValue}

/**
 * maps a logger specific log event to a json structure
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait EventMapper[T] {

  private val registry: MapperFunctionRegistry[T] = MapperFunctionRegistry[T](conf)

  def conf: Configuration

  private def toMap(event: T): Map[String, Any] = registry.execute(event)

  /**
   * maps a given log event to a json string
   *
   * @param event the event to map
   * @return json string representation of given event
   */
  final def mapToString(event: T): String = compact(map(event))

  /**
   * maps a given log event to json
   *
   * @param event the event to map
   * @return json representation of given event
   */
  final def map(event: T): JValue = decompose(toMap(event))(DefaultFormats)

  /**
   * helper to transform null value to empty string.
   *
   * @param value the value to return if not null
   * @return given value (if not null) or empty string
   */
  protected def nullToEmpty(value: String): Any = nullToDefault(value, "")

  /**
   * helper to transform null value to nothing which removes the key/value from json.
   *
   * @param value the value to return if not null
   * @return given value (if not null) or nothing
   */
  protected def nullToNothing[X](value: X): Any = nullToDefault(value, nothing)

  /**
   * helper to transform null value to default value.
   *
   * @param value the value to return if not null
   * @return given value (if not null) or default
   */
  protected def nullToDefault[X](value: X, default: Any): Any = if (value != null) {
    value
  } else {
    if (default == null) {
      nothing
    } else {
      default
    }
  }

  /**
   * @return nothing hint to remove empty values
   */
  protected def nothing: JValue = JNothing

  /**
   * hook to register mapper functions
   *
   * @param registry the registry to register mappers at
   * @return given registry for chaining
   */
  protected def registerMapper(registry: MapperFunctionRegistry[T]): MapperFunctionRegistry[T]

  registerMapper(registry)
    .register(Fields.SERVICE, event => conf.serviceName)
    .register(Fields.HOSTNAME, event => conf.hostName).fixate()
}



