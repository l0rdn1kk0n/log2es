package de.agilecoders.logback.elasticsearch

import de.agilecoders.logback.elasticsearch.actor.LogbackActorSystem

/**
 * holder class for common instances
 *
 * @author miha
 */
object LogbackContext {

    lazy val system = LogbackActorSystem.instance

    lazy val configuration = LogbackActorSystem.configuration

    /**
     * shut down logback and actor context
     */
    def shutdown() {
        LogbackActorSystem.shutdown()
    }

}
