package de.agilecoders.logback.elasticsearch.conf

import de.agilecoders.logback.elasticsearch.mapper.{LoggingEventToXContentMapper, LoggingEventMapper}
import org.elasticsearch.common.xcontent.XContentBuilder
import de.agilecoders.logback.elasticsearch.store.{Store, BufferedStore}
import org.elasticsearch.action.index.IndexRequest
import akka.actor.{ActorRef, ActorContext}
import de.agilecoders.logback.elasticsearch.actor.Creator
import de.agilecoders.logback.elasticsearch.ElasticSearchLogbackAppender

/**
 * Trait that can be appended to classes/objects that needs access to the dependencies.
 *
 * @author miha
 */
trait DependencyHolder {

    import de.agilecoders.logback.elasticsearch.conf.Dependencies

    /**
     * holds the dependency holder instance
     */
    protected lazy val dependencies: Dependencies = newDependencyHolder()

    /**
     * creates a new dependency holder instance. By default: `DefaultDependencyHolder`
     */
    protected def newDependencyHolder(): Dependencies = DefaultDependencyHolder()
}

/**
 * Default `Dependencies` implementation.
 *
 * @author miha
 */
case class DefaultDependencyHolder() extends Dependencies {

    override lazy val configuration: Configuration = Configuration.instance

    override lazy val mapper: LoggingEventMapper[XContentBuilder] = LoggingEventToXContentMapper(configuration)

    override def newStoreClient(): BufferedStore[XContentBuilder, IndexRequest] = Store.newClient()

    override def newConverter(context: ActorContext) = Creator.newConverter(context)

    override def newSender(context: ActorContext) = Creator.newIndexSender(context)

    override def newWorker(context: ActorContext): ActorRef = Creator.newWorker(context)

    override def newErrorHandler(context: ActorContext, appender: ElasticSearchLogbackAppender): ActorRef = Creator.newErrorHandler(context, appender)
}