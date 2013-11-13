package de.agilecoders.elasticsearch.logger.core

import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import de.agilecoders.elasticsearch.logger.core.messages.Initialize
import de.agilecoders.elasticsearch.logger.core.store.Store
import org.elasticsearch.transport.TransportRequest

/**
 * holder class for common instances
 *
 * @author miha
 */
object Log2esContext {

    /**
     * TODO
     */
    def create[EventT, DataT, RequestT <: TransportRequest](dependencies: Dependencies[EventT, DataT, RequestT]): Log2esContext[EventT, DataT, RequestT] = {
        val context = Log2esContext(dependencies)

        context
    }

}

case class Log2esContext[EventT, DataT, RequestT <: TransportRequest](dependencies: Dependencies[EventT, DataT, RequestT]) extends Lifecycle[Log2esContext[EventT, DataT, RequestT]] {
    private[this] lazy val actorSystem = dependencies.actorSystem

    /**
     * starts the log2es context
     */
    def start(): Log2esContext[EventT, DataT, RequestT] = {
        Store.connect()
        actorSystem.start()

        actorSystem.reaper ! Initialize(this)

        this
    }

    /**
     * shut down log2es context
     */
    def stop() {
        dependencies.actorSystem.stop()
        Store.disconnect()
    }

    /**
     * asks for shut down of log2es context
     */
    def shutdownAndAwaitTermination(): Unit = {
        dependencies.actorSystem.sendPoisonPill()
    }
}
