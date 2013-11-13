package de.agilecoders.elasticsearch.logger.log4j2.mapper

import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import de.agilecoders.elasticsearch.logger.core.mapper.{Keys, LoggingEventMapper}
import java.lang.Object
import org.apache.logging.log4j.core.LogEvent
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
    override def isSupported(event: AnyRef) = event != null && event.isInstanceOf[LogEvent]

    /**
     * transforms an `LoggingEvent` to a `Map` according to the
     * elasticsearch mapping.
     *
     * @param event the logging event to transform
     * @return logging event as map
     */
    override def map(rawEvent: AnyRef) = {
        val event = rawEvent.asInstanceOf[LogEvent]
        val builder = JsonXContent.contentBuilder().startObject()

        addBaseValues(builder, event)

        // NOTE: there's no arguments support

        val mdc = event.getContextMap
        if (configuration.addMdc && mdc != null && !mdc.isEmpty) {
            builder.field(Keys.mdc, mdc)
        }

        if (configuration.addCaller && event.getFQCN != null) {
            builder.field(Keys.caller, event.getFQCN)
        }

        if (configuration.addStacktrace && event.getThrown != null) {
            builder.field(Keys.stacktrace, transformThrowable(event.getThrown))
        }

        builder.endObject()
    }

    private def transformThrowable(throwable: Throwable): Map[String, Object] = {
        val map = mutable.Map[String, Object]()

        map.put(Keys.clazz, throwable.getClass.getName)
        map.put(Keys.message, throwable.getMessage)
        map.put(Keys.stacktrace, throwable.getStackTrace) // TODO

        if (throwable.getCause != null) {
            map.put(Keys.cause, transformThrowable(throwable.getCause))
        }

        map.toMap
    }


    /**
     * adds a set of base values to given json structure.
     *
     * @param json  the json structure (map) to add values to.
     * @param event the logging event.
     */
    private def addBaseValues(builder: XContentBuilder, event: LogEvent) {
        if (configuration.addTimestamp) {
            builder.field(Keys.timestamp, event.getMillis)
        }

        if (configuration.addDate) {
            builder.field(Keys.date, new DateTime(event.getMillis).toString(formatter))
        }

        if (configuration.addLevel && event.getLevel != null) {
            builder.field(Keys.level, event.getLevel.toString)
        }

        if (configuration.addMessage && event.getMessage != null) {
            builder.field(Keys.message, event.getMessage)
        }

        if (configuration.addLogger && event.getLoggerName != null) {
            builder.field(Keys.logger, event.getLoggerName)
        }

        if (configuration.addThread && event.getThreadName != null) {
            builder.field(Keys.thread, event.getThreadName)
        }

        if (configuration.addMarker && event.getMarker != null) {
            builder.field(Keys.thread, event.getMarker.getName)
        }
    }

}