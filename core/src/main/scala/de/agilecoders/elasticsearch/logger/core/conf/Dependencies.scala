package de.agilecoders.elasticsearch.logger.core.conf

import akka.actor.{ActorContext, ActorRef}
import de.agilecoders.elasticsearch.logger.core.Log2esAppender
import de.agilecoders.elasticsearch.logger.core.actor.{Creator, LogbackActorSystem}
import de.agilecoders.elasticsearch.logger.core.mapper.LoggingEventMapper
import de.agilecoders.elasticsearch.logger.core.store.{Store, BufferedStore}
import de.agilecoders.logback.elasticsearch.actor.Creator
import org.elasticsearch.transport.TransportRequest

/**
 * `Dependencies` is a trait that defines all possible dependencies that can be created and "injected".
 *
 * @author miha
 */
trait Dependencies[EventT, DataT, RequestT <: TransportRequest] {

    /**
     * holds the logback actor system instance
     */
    lazy val actorSystem: LogbackActorSystem = newActorSystem(configuration)

    /**
     * creates a new configuration instance
     */
    protected def newActorSystem(configuration: Configuration): LogbackActorSystem = LogbackActorSystem(configuration)

    /**
     * holds the configuration instance
     */
    lazy val configuration: Configuration = newConfiguration()

    /**
     * creates a new configuration instance
     */
    protected def newConfiguration(): Configuration = AkkaBasedConfiguration()

    /**
     * holds the mapper instance
     */
    lazy val mapper: LoggingEventMapper[DataT] = newMapper()

    /**
     * creates a new mapper instance
     */
    protected def newMapper(): LoggingEventMapper[DataT]

    /**
     * holds the actor creator instance
     */
    lazy val actorCreator: Creator[EventT] = newActorCreator(actorSystem)

    /**
     * creates a new actor creator instance
     */
    protected def newActorCreator(actorSystem: LogbackActorSystem): Creator[EventT] = Creator(actorSystem)

    /**
     * creates a new store client instance
     */
    def newStoreClient(): BufferedStore[DataT, RequestT] = Store.newClient(configuration)

    /**
     * creates a new converter actor reference
     */
    def newConverter(context: ActorContext): ActorRef = actorCreator.newConverter(context)

    /**
     * creates a new sender actor reference
     */
    def newSender(context: ActorContext): ActorRef = actorCreator.newIndexSender(context)

    /**
     * creates a new worker actor reference
     */
    def newWorker(context: ActorContext): ActorRef = actorCreator.newWorker(context)

    /**
     * creates a new router actor reference
     */
    def newRouter(appender: Log2esAppender[EventT]): ActorRef = actorCreator.newRouter(appender)

    /**
     * creates a new error handler actor reference
     *
     * @param appender the logback appender
     */
    def newErrorHandler(context: ActorContext): ActorRef
}