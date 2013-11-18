package de.agilecoders.elasticsearch.logger.log4j2

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.impl.Log4jLogEvent
import org.apache.logging.log4j.message.Message
import org.joda.time.DateTime

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
object Factory {
    def newEvent(): LogEvent = newEvent(scala.math.random)

    def newEvent(number: Double): LogEvent = {
        val event = new Log4jLogEvent(DateTime.now().getMillis) {
            val message = new Message() {
                def getFormattedMessage = "message-" + number

                def getFormat = ???

                def getParameters = ???

                def getThrowable = ???
            }

            override def getMessage = message

            override def getThreadName = "thread-" + number

            override def getLevel = Level.WARN

            override def getLoggerName = "logger-" + number
        }

        event
    }
}
