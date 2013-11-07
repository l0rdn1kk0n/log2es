package de.agilecoders.logback.elasticsearch

import akka.actor.{Props, ActorSystem}
import de.agilecoders.logback.elasticsearch.actor.{ShutdownReaper, LogbackActorSystem}
import de.agilecoders.logback.elasticsearch.conf.Configuration
import de.agilecoders.logback.elasticsearch.store.Store

/**
 * holder class for common instances
 *
 * @author miha
 */
object Log2esContext {
    lazy val system: ActorSystem = {
        Store.connect()
        val system = LogbackActorSystem.start()

        system.actorOf(Props(classOf[ShutdownReaper]), "reaper")

        system.log.info("startup Log2esContext")
        system
    }

    lazy val configuration: Configuration = Configuration.instance

    /**
     * shut down log2es context
     */
    def shutdown() {
        system.log.info("shutdown Log2esContext")

        LogbackActorSystem.shutdown()
        Store.disconnect()
    }

    /**
     * asks for shut down of log2es context
     */
    def shutdownAndAwaitTermination(): Unit = {
        system.log.info("shutdown and await termination of Log2esContext")

        LogbackActorSystem.sendPoisonPill()
    }

}
