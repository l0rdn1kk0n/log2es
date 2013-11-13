package de.agilecoders.elasticsearch.logger.log4j.mapper

import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import de.agilecoders.elasticsearch.logger.core.mapper.{Keys, LoggingEventMapper}
import java.lang.Object
import org.apache.log4j.spi.LocationInfo
import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.spi.ThrowableInformation
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.json.JsonXContent
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import scala.Predef.String
import scala.collection.Map
import scala.collection.mutable

/**
 * TODO miha
 */
@SerialVersionUID(1687691786811587371L)
case class LoggingEventToXContentMapper(configuration: Configuration) extends LoggingEventMapper[XContentBuilder] {
    private[this] val formatter: DateTimeFormatter = ISODateTimeFormat.dateTime()

    /**
     * @param event the logging event to check for support
     * @return true, if given event can be transformed
     */
    override def isSupported(event: AnyRef) = event != null && event.isInstanceOf[LoggingEvent]

    /**
     * transforms an `LoggingEvent` to a `Map` according to the
     * elasticsearch mapping.
     *
     * @param event the logging event to transform
     * @return logging event as map
     */
    override def map(rawEvent: AnyRef) = {
        val event = rawEvent.asInstanceOf[LoggingEvent]
        val builder = JsonXContent.contentBuilder().startObject()

        addBaseValues(builder, event)

        // NOTE: there's no arguments support

        val mdc = event.getProperties
        if (configuration.addMdc && mdc != null && !mdc.isEmpty) {
            builder.field(Keys.mdc, mdc)
        }

        if (configuration.addCaller && event.getLocationInformation != null) {
            builder.field(Keys.caller, transformCaller(event.getLocationInformation))
        }

        if (configuration.addStacktrace && event.getThrowableInformation != null) {
            builder.field(Keys.stacktrace, transformThrowable(event.getThrowableInformation))
        }

        builder.endObject()
    }

    private def transformCaller(stackTraceElement: LocationInfo): Map[String, Object] = {
        val map = mutable.Map[String, Object]()

        map.put(Keys.file, stackTraceElement.getFileName)
        map.put(Keys.clazz, stackTraceElement.getClassName)
        map.put(Keys.method, stackTraceElement.getMethodName)
        map.put(Keys.line, stackTraceElement.getLineNumber.asInstanceOf[Integer])

        map.toMap
    }

    private def transformThrowable(throwable: ThrowableInformation): Map[String, Object] = {
        val map = mutable.Map[String, Object]()

        map.put(Keys.clazz, throwable.getThrowable.getClass.getName)
        map.put(Keys.message, throwable.getThrowable.getMessage)
        map.put(Keys.stacktrace, throwable.getThrowableStrRep)

        if (throwable.getThrowable.getCause != null) {
            map.put(Keys.cause, transformThrowable(new ThrowableInformation(throwable.getThrowable.getCause)))
        }

        map.toMap
    }


    /**
     * adds a set of base values to given json structure.
     *
     * @param json  the json structure (map) to add values to.
     * @param event the logging event.
     */
    private def addBaseValues(builder: XContentBuilder, event: LoggingEvent) {
        if (configuration.addTimestamp) {
            builder.field(Keys.timestamp, event.getTimeStamp)
        }

        if (configuration.addDate) {
            builder.field(Keys.date, new DateTime(event.getTimeStamp).toString(formatter))
        }

        if (configuration.addLevel && event.getLevel != null) {
            builder.field(Keys.level, event.getLevel.toString)
        }

        if (configuration.addMessage && event.getMessage != null) {
            builder.field(Keys.message, event.getRenderedMessage)
        }

        if (configuration.addLogger && event.getLoggerName != null) {
            builder.field(Keys.logger, event.getLoggerName)
        }

        if (configuration.addThread && event.getThreadName != null) {
            builder.field(Keys.thread, event.getThreadName)
        }

        // NOTE miha: log4j hasn't marker support
    }

}