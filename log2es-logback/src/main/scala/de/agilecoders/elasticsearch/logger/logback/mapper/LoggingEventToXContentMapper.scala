package de.agilecoders.elasticsearch.logger.logback.mapper

import ch.qos.logback.classic.spi.{StackTraceElementProxy, IThrowableProxy, ILoggingEvent}
import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import de.agilecoders.elasticsearch.logger.core.mapper.{Keys, LoggingEventMapper}
import java.lang.Object
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.json.JsonXContent
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import scala.Predef.String
import scala.collection.Map
import scala.collection.convert.WrapAsJava._
import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * The `LoggingEventToXContentMapper` maps incoming `ILoggingEvent`s to a `XContentBuilder` which
 * is used by elasticsearch client to create an element.
 */
@SerialVersionUID(1687691786811587371L)
case class LoggingEventToXContentMapper(configuration: Configuration) extends LoggingEventMapper {
    private[this] val formatter: DateTimeFormatter = ISODateTimeFormat.dateTime()

    /**
     * @param event the logging event to check for support
     * @return true, if given event can be transformed
     */
    override def isSupported(event: AnyRef) = event != null && event.isInstanceOf[ILoggingEvent]

    /**
     * transforms an `ILoggingEvent` to a `Map` according to the
     * elasticsearch mapping.
     *
     * @param event the logging event to transform
     * @return logging event as map
     */
    override def map(rawEvent: AnyRef) = {
        val event = rawEvent.asInstanceOf[ILoggingEvent]
        val builder = JsonXContent.contentBuilder().startObject()

        addBaseValues(builder, event)

        if (configuration.addArguments && event.getArgumentArray != null && event.getArgumentArray.length > 0) {
            val arr = map(event.getArgumentArray, {
                v: Object => v.toString
            })

            builder.field(Keys.arguments, arr)
        }

        val mdc = event.getMDCPropertyMap
        if (configuration.addMdc && mdc != null && !mdc.isEmpty) {
            builder.field(Keys.mdc, mdc)
        }

        if (configuration.addCaller && event.hasCallerData) {
            builder.field(Keys.caller, map(event.getCallerData, transformCaller))
        }

        if (configuration.addStacktrace && event.getThrowableProxy != null) {
            builder.field(Keys.stacktrace, transformThrowable(event.getThrowableProxy))
        }

        builder.endObject()
    }

    private def transformCaller(stackTraceElement: StackTraceElement): Map[String, Object] = {
        val map = mutable.Map[String, Object]()

        map.put(Keys.file, stackTraceElement.getFileName)
        map.put(Keys.clazz, stackTraceElement.getClassName)
        map.put(Keys.method, stackTraceElement.getMethodName)
        map.put(Keys.line, stackTraceElement.getLineNumber.asInstanceOf[Integer])
        map.put(Keys.nativeValue, stackTraceElement.isNativeMethod.asInstanceOf[java.lang.Boolean])

        map.toMap
    }

    private def map[T: ClassTag, O: ClassTag](array: Array[T], transformer: T => O): java.lang.Iterable[O] = {
        for (i <- Range(0, array.length)) yield transformer(array(i))
    }

    private def transformThrowable(throwable: IThrowableProxy): Map[String, Object] = {
        val map = mutable.Map[String, Object]()

        map.put(Keys.clazz, throwable.getClassName)
        map.put(Keys.message, throwable.getMessage)
        map.put(Keys.stacktrace, transformStackTraceElement(throwable))

        if (throwable.getCause != null) {
            map.put(Keys.cause, transformThrowable(throwable.getCause))
        }

        map.toMap
    }


    private def transformStackTraceElement(throwable: IThrowableProxy): Seq[String] = {
        val elementProxies: Array[StackTraceElementProxy] = throwable.getStackTraceElementProxyArray
        val totalFrames = elementProxies.length - throwable.getCommonFrames

        for (i <- Range(0, totalFrames)) yield elementProxies(i).getStackTraceElement.toString
    }

    /**
     * adds a set of base values to given json structure.
     *
     * @param json  the json structure (map) to add values to.
     * @param event the logging event.
     */
    private def addBaseValues(builder: XContentBuilder, event: ILoggingEvent) {
        if (configuration.addTimestamp) {
            builder.field(Keys.timestamp, event.getTimeStamp)
        }

        if (configuration.addDate) {
            builder.field(Keys.date, new DateTime(event.getTimeStamp).toString(formatter))
        }

        if (configuration.addLevel && event.getLevel != null) {
            builder.field(Keys.level, event.getLevel.levelStr)
        }

        if (configuration.addMessage && event.getMessage != null) {
            builder.field(Keys.message, event.getFormattedMessage)
        }

        if (configuration.addLogger && event.getLoggerName != null) {
            builder.field(Keys.logger, event.getLoggerName)
        }

        if (configuration.addThread && event.getThreadName != null) {
            builder.field(Keys.thread, event.getThreadName)
        }

        if (configuration.addMarker && event.getMarker != null) {
            builder.field(Keys.marker, event.getMarker.getName)
        }

    }

}