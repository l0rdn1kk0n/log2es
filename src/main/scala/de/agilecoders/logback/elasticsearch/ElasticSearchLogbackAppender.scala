package de.agilecoders.logback.elasticsearch

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{Appender, UnsynchronizedAppenderBase}
import de.agilecoders.logback.elasticsearch.actor.Creator
import akka.actor.PoisonPill

/**
 * Special `ch.qos.logback.core.Appender` that is able to send all
 * log messages to an elasticsearch cluster. All log events will be handled
 * asynchronous.
 * <p/>
 * In order to optimize performance this appender deems events of level TRACE,
 * DEBUG and INFO as discardable
 *
 * @author miha
 */
class ElasticSearchLogbackAppender extends UnsynchronizedAppenderBase[ILoggingEvent] with IElasticSearchLogbackAppender {
    lazy val router = Creator.newRouter(this)
    lazy val configuration = LogbackContext.configuration

    /**
     * sends given logging event to actor system
     *
     * @param eventObject log event to handle
     */
    override def append(eventObject: ILoggingEvent): Unit = {
        router ! eventObject
    }

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    override def isDiscardable(event: ILoggingEvent): Boolean = {
        event.getLevel.toInt <= configuration.discardableLevel
    }

    /**
     * shutdown whole context
     */
    override def stop() {
        if (isStarted) {
            super.stop()

            router ! PoisonPill.getInstance
        }
    }
}

/**
 * common base interface of java and scala appender
 *
 * @author miha
 */
trait IElasticSearchLogbackAppender extends Appender[ILoggingEvent] {

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    def isDiscardable(event: ILoggingEvent): Boolean
}