package de.agilecoders.elasticsearch.logger.core.conf

import de.agilecoders.elasticsearch.logger.core.actor.LogbackActorSystem
import de.agilecoders.elasticsearch.logger.core.mapper.LoggingEventMapper
import de.agilecoders.elasticsearch.logger.core.store.{Store, BufferedStore}

/**
 * `Dependencies` is a trait that defines all possible dependencies that can be created and "injected".
 *
 * @author miha
 */
trait Dependencies {

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
     * creates a new mapper instance
     */
    def newMapper(): LoggingEventMapper

    /**
     * creates a new store client instance
     */
    def newStoreClient(): BufferedStore = Store.newClient(configuration)

}