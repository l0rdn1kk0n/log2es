package de.agilecoders.elasticsearch.logger.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.actor.{Names, Router}
import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import de.agilecoders.elasticsearch.logger.core.messages.Initialize
import de.agilecoders.elasticsearch.logger.logback.mapper.LoggingEventToXContentMapper

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
    val self = this
    protected[logback] lazy val log2es = Log2esContext.create(new Dependencies {
        override def newMapper() = LoggingEventToXContentMapper(configuration)
    })
    protected[logback] lazy val router = {
        val router = log2es.dependencies.actorSystem.actorOf(Router.props(), Names.Router)
        router ! Initialize(log2es)
        router
    }
    protected[logback] lazy val discardableLevel = Level.toLevel(log2es.dependencies.configuration.discardable).levelInt

    /**
     * sends given logging event to actor system
     *
     * @param eventObject log event to handle
     */
    override def append(eventObject: ILoggingEvent): Unit = {
        if (log2es.dependencies.configuration.addThread) {
            eventObject.getThreadName
        }

        if (log2es.dependencies.configuration.addMdc) {
            eventObject.getMDCPropertyMap
        }

        router ! eventObject
    }

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    override def isDiscardable(event: ILoggingEvent): Boolean = {
        event.getLevel.toInt <= discardableLevel
    }

    override def stop() {
        if (isStarted) {
            super.stop()

            log2es.shutdownAndAwaitTermination()
        }
    }

}
