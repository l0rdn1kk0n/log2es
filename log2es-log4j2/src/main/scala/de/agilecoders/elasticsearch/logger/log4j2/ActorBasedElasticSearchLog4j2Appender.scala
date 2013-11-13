package de.agilecoders.elasticsearch.logger.log4j2

import akka.actor.ActorContext
import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import de.agilecoders.elasticsearch.logger.log4j2.ElasticSearchLog4j2Appender
import de.agilecoders.elasticsearch.logger.log4j2.mapper.LoggingEventToXContentMapper
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentBuilder


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
class ActorBasedElasticSearchLog4j2Appender extends AbstractAppender("log2es", null, null) with ElasticSearchLog4j2Appender {
    val self = this
    protected[log4j] lazy val context = Log2esContext.create(new Dependencies[LogEvent, XContentBuilder, IndexRequest] {
        override protected def newMapper() = LoggingEventToXContentMapper(configuration)

        override def newErrorHandler(context: ActorContext) = actorCreator.newErrorHandler(context, self)
    })
    protected[log4j] lazy val router = context.dependencies.newRouter(this)
    protected[log4j] lazy val discardableLevel = Level.toLevel(context.dependencies.configuration.discardable).intLevel()

    /**
     * sends given logging event to actor system
     *
     * @param eventObject log event to handle
     */
    override def append(eventObject: LogEvent): Unit = {
        router ! eventObject
    }

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    override def isDiscardable(event: LogEvent): Boolean = {
        event.getLevel.intLevel() <= discardableLevel
    }

    override def stop() {
        super.stop()

        context.shutdownAndAwaitTermination()
    }

    def addInfo(msg: String) = ???

    def addInfo(msg: String, ex: Throwable) = ???

    def addWarn(msg: String) = ???

    def addWarn(msg: String, ex: Throwable) = ???

    def addError(msg: String) = ???

    def addError(msg: String, ex: Throwable) = ???
}
