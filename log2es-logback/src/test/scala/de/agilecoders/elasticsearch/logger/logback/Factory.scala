package de.agilecoders.elasticsearch.logger.logback

import org.joda.time.DateTime
import ch.qos.logback.classic.spi.{LoggingEvent, ILoggingEvent}
import ch.qos.logback.classic.Level

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
object Factory {
    def newEvent(): ILoggingEvent = newEvent(scala.math.random)

    def newEvent(number: Double): ILoggingEvent = {
        val event = new LoggingEvent()

        event.setMessage("message" + number)
        event.setThreadName("thread" + number)
        event.setLevel(Level.WARN)
        event.setLoggerName("logger" + number)
        event.setTimeStamp(DateTime.now().getMillis)

        event
    }
}
