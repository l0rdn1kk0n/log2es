package de.agilecoders.elasticsearch.logger.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import de.agilecoders.elasticsearch.logger.core.{Log2esAppender, Log2esContext}
import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import akka.actor.ActorContext
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.action.index.IndexRequest
import de.agilecoders.elasticsearch.logger.logback.mapper.LoggingEventToXContentMapper
import de.agilecoders.elasticsearch.logger.core.actor.Creator
import de.agilecoders.elasticsearch.logger.logback.actor.ErrorHandler

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
    protected[logback] lazy val context = Log2esContext.create(new Dependencies[ILoggingEvent, XContentBuilder, IndexRequest] {
        override protected def newMapper() = LoggingEventToXContentMapper(configuration)
        override def newErrorHandler(context: ActorContext) = context.actorOf(ErrorHandler.props(self))
    })
    protected[logback] lazy val router = context.dependencies.newRouter(this)
    protected[logback] lazy val discardableLevel = Level.toLevel(context.dependencies.configuration.discardable).levelInt

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
        event.getLevel.toInt <= discardableLevel
    }

    override def stop() {
        if (isStarted) {
            super.stop()

            context.shutdownAndAwaitTermination()
        }
    }

}
