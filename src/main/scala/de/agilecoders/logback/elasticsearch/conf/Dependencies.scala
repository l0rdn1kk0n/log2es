package de.agilecoders.logback.elasticsearch.conf

import akka.actor.{ActorContext, ActorRef}
import de.agilecoders.logback.elasticsearch.ElasticSearchLogbackAppender
import de.agilecoders.logback.elasticsearch.actor.Creator
import de.agilecoders.logback.elasticsearch.mapper.{LoggingEventToXContentMapper, LoggingEventMapper}
import de.agilecoders.logback.elasticsearch.store.{Store, BufferedStore}
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * `Dependencies` is a trait that defines all possible dependencies that can be created and "injected".
 *
 * @author miha
 */
trait Dependencies {
    /**
     * creates a new configuration instance
     */
    def configuration: Configuration

    /**
     * creates a new mapper instance
     */
    def mapper: LoggingEventMapper[XContentBuilder]

    /**
     * creates a new store client instance
     */
    def newStoreClient(): BufferedStore[XContentBuilder, IndexRequest]

    /**
     * creates a new converter actor reference
     */
    def newConverter(context: ActorContext): ActorRef

    /**
     * creates a new sender actor reference
     */
    def newSender(context: ActorContext): ActorRef

    /**
     * creates a new worker actor reference
     */
    def newWorker(context: ActorContext): ActorRef

    /**
     * creates a new error handler actor reference
     *
     * @param appender the logback appender
     */
    def newErrorHandler(context: ActorContext, appender: ElasticSearchLogbackAppender): ActorRef
}