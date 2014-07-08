package de.agilecoders.logger.log2es.core.mapper

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.common.EventParseException

import scala.collection.mutable

/**
 * registry for all event mapper functions
 *
 * @tparam T the logging event type
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class MapperFunctionRegistry[T](conf: Configuration) {
  private val construct = mutable.Map[String, T => Any]()
  private lazy val mapper = construct.toMap
  private lazy val fields = conf.fields

  /**
   * transform internal mutable map into a immutable map
   */
  def fixate() = {
    mapper.hashCode()
    construct.clear()
  }

  /**
   * registers a new mapper function for a special field, if there's already a mapper set for given field,
   * it will be overridden.
   *
   * @param field the field the mapper function is related to
   * @param f the mapper function for given field
   * @return this instance for chaining
   */
  def register(field: String, f: T => Any): MapperFunctionRegistry[T] = if (fields.contains(field)) {
    construct.put(field, f)
    this
  } else {
    this
  }

  /**
   * executes all registered mapper for given event
   * @param event the event to map
   * @return mapped event as map
   */
  def execute(event: T): Map[String, Any] = {
    val map = mutable.Map[String, Any]()

    try {
      mapper.foreach(p => map.put(p._1, p._2(event)))
    } catch {
      case t: Throwable => throw new EventParseException(t)
    }

    map.toMap
  }
}
