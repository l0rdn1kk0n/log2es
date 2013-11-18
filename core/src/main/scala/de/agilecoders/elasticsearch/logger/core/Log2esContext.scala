package de.agilecoders.elasticsearch.logger.core

import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import de.agilecoders.elasticsearch.logger.core.messages.Initialize

/**
 * holder class for common instances
 *
 * @author miha
 */
object Log2esContext {

    /**
     * TODO
     */
    def create(dependencies: Dependencies): Log2esContext = {
        val context = Log2esContext(dependencies)

        context.start()
    }

}

case class Log2esContext(dependencies: Dependencies) extends Lifecycle[Log2esContext] {
    private[this] lazy val actorSystem = dependencies.actorSystem

    /**
     * starts the log2es context
     */
    def start(): Log2esContext = {
        actorSystem.start()

        actorSystem.reaper ! Initialize(this)

        this
    }

    /**
     * shut down log2es context
     */
    def stop() {
        dependencies.actorSystem.stop()
    }

    /**
     * asks for shut down of log2es context
     */
    def shutdownAndAwaitTermination(): Unit = {
        dependencies.actorSystem.sendPoisonPill()
    }
}
