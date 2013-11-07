package de.agilecoders.logback.elasticsearch.mapper

import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * Defines the interface of all `ILoggingEvent` to `Map`
 * transformer. This transformer is used to create a json representation
 * of `ILoggingEvent` that matches the elasticsearch mapping.
 *
 * @author miha
 */
trait LoggingEventMapper[T <: AnyRef] {

    /**
     * transforms an `ILoggingEvent` to a `Map` according to the
     * elasticsearch mapping.
     *
     * @param event the logging event to transform
     * @return logging event as map
     */
    def map(event: ILoggingEvent): T

}

/**
 * defines all available keys of json structure.
 */
case object Keys {
    val line = "line"
    val mdc = "mdc"
    val arguments = "arguments"
    val cause = "cause"
    val caller = "caller"
    val file = "file"
    val clazz = "class"
    val method = "method"
    val nativeValue = "native"
    val stacktrace = "stacktrace"
    val timestamp = "timestamp"
    val marker = "marker"
    val message = "message"
    val level = "level"
    val logger = "logger"
    val thread = "thread"
    val date = "date"
}