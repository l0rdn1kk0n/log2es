package de.agilecoders.elasticsearch.logger.log4j

import akka.actor.ActorContext
import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.spi.LoggingEvent
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentBuilder
import de.agilecoders.elasticsearch.logger.log4j.mapper.LoggingEventToXContentMapper

/**
 * Special `org.apache.log4j.Appender` that is able to send all
 * log messages to an elasticsearch cluster. All log events will be handled
 * asynchronous.
 * <p/>
 * In order to optimize performance this appender deems events of level TRACE,
 * DEBUG and INFO as discardable
 *
 * @author miha
 */
class ActorBasedElasticSearchLog4jAppender extends AppenderSkeleton with ElasticSearchLog4jAppender {
    val self = this
    protected[log4j] lazy val context = Log2esContext.create(new Dependencies[LoggingEvent, XContentBuilder, IndexRequest] {
        override protected def newMapper() = LoggingEventToXContentMapper(configuration)

        override def newErrorHandler(context: ActorContext) = actorCreator.newErrorHandler(context, self)
    })
    protected[log4j] lazy val router = context.dependencies.newRouter(this)
    protected[log4j] lazy val discardableLevel = Level.toLevel(context.dependencies.configuration.discardable).toInt

    /**
     * sends given logging event to actor system
     *
     * @param eventObject log event to handle
     */
    override def append(eventObject: LoggingEvent): Unit = {
        if (context.dependencies.configuration.addMdc) {
            // because each message is sent asynchronous in a different thread it must be
            // ensured that mdc data is copied from thread context to event object.
            eventObject.getMDCCopy()
        }

        router ! eventObject
    }

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    override def isDiscardable(event: LoggingEvent): Boolean = {
        event.getLevel.toInt <= discardableLevel
    }

    override def close() {
        if (!closed) {
            super.close()

            context.shutdownAndAwaitTermination()
        }
    }

    def addInfo(msg: String) = ???

    def addInfo(msg: String, ex: Throwable) = ???

    def addWarn(msg: String) = ???

    def addWarn(msg: String, ex: Throwable) = ???

    def addError(msg: String) = ???

    def addError(msg: String, ex: Throwable) = ???

    override def requiresLayout() = false
}
