package de.agilecoders.logback.elasticsearch.appender

import akka.actor.{ActorSystem, Props}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import de.agilecoders.logback.elasticsearch.actor.{ShutdownReaper, Creator}
import de.agilecoders.logback.elasticsearch.{ElasticSearchLogbackAppender, Log2esContext}

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
class ActorBasedElasticSearchLogbackAppender extends UnsynchronizedAppenderBase[ILoggingEvent] with ElasticSearchLogbackAppender {
    protected[elasticsearch] lazy val router = Creator.newRouter(this)
    protected[elasticsearch] lazy val configuration = Log2esContext.configuration

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

    override def stop() {
        if (isStarted) {
            super.stop()

            Log2esContext.shutdownAndAwaitTermination()
        }
    }

}
