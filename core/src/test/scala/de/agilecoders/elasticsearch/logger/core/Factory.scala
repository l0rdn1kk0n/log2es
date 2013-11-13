package de.agilecoders.elasticsearch.logger.core

import org.joda.time.DateTime

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
