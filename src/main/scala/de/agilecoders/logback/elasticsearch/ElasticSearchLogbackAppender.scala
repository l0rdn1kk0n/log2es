package de.agilecoders.logback.elasticsearch

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender

/**
 * common base interface of java and scala appender
 *
 * @author miha
 */
trait ElasticSearchLogbackAppender extends Appender[ILoggingEvent] {

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    def isDiscardable(event: ILoggingEvent): Boolean
}