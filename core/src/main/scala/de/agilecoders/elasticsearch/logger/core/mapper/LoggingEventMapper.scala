package de.agilecoders.elasticsearch.logger.core.mapper

/**
 * Defines the interface of all `ILoggingEvent` to `Map`
 * transformer. This transformer is used to create a json representation
 * of `ILoggingEvent` that matches the elasticsearch mapping.
 *
 * @author miha
 */
trait LoggingEventMapper[T <: AnyRef] {

    /**
     * transforms an `event` to a `data` structure according to the
     * elasticsearch mapping.
     *
     * @param event the logging event to transform
     * @return logging event as map
     */
    def map(event: AnyRef): T

    /**
     * @param event the logging event to check for support
     * @return true, if given event can be transformed
     */
    def isSupported(event: AnyRef): Boolean

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