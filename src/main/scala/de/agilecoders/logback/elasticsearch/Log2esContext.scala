package de.agilecoders.logback.elasticsearch

import akka.actor.{PoisonPill, ActorRef, ActorSystem}
import com.twitter.ostrich.stats.Stats
import de.agilecoders.logback.elasticsearch.actor.LogbackActorSystem
import de.agilecoders.logback.elasticsearch.conf.Configuration
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import de.agilecoders.logback.elasticsearch.actor.Reaper.WatchMe
import de.agilecoders.logback.elasticsearch.store.Store

/**
 * holder class for common instances
 *
 * @author miha
 */
object Log2esContext {

    private[this] var _alive = false
    private[this] var _watcher:ActorRef = null

    lazy val system: ActorSystem = LogbackActorSystem.instance
    lazy val configuration: Configuration = Configuration.instance

    def alive(): Boolean = _alive

    private[this] def watcher(ref: ActorRef): ActorRef = {
        _watcher = ref
        ref
    }

    def watchMe(ref: ActorRef): ActorRef = {
       _watcher ! WatchMe(ref)
        ref
    }

    /**
     * starts up the log2es context
     */
    def start(ref: ActorRef): ActorSystem = {
        Store.connect()

        _alive = true
        val system = LogbackActorSystem.start()

        watcher(ref)
        system
    }

    /**
     * shut down log2es context
     */
    def shutdown() {
        LogbackActorSystem.shutdown()
        _watcher = null

        Store.disconnect()

        _alive = false
    }

    /**
     * asks for shut down of log2es context
     */
    def shutdownAndAwaitTermination(): Unit = {
        LogbackActorSystem.sendPoisonPill()
    }

}
