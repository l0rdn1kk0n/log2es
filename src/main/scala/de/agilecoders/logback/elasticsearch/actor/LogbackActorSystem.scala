package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{PoisonPill, ActorSystem}
import de.agilecoders.logback.elasticsearch.conf.Configuration

/**
 * Holds the `ActorSystem` instance.
 *
 * @author miha
 */
object LogbackActorSystem {

    lazy val instance: ActorSystem = ActorSystem.create(Configuration.name, Configuration.instance.file)

    /**
     * starts up the actor system
     */
    def start(): ActorSystem = instance

    /**
     * shutdown actor system
     */
    def shutdown(): Unit = {
        sendPoisonPill()

        instance.shutdown()
        instance.awaitTermination()
    }

    /**
     * sends poison pill to all actors
     */
    def sendPoisonPill(): Unit = {
        instance.eventStream.publish(PoisonPill.getInstance)
        instance.actorSelection("/user/router") ! PoisonPill.getInstance
    }

}
