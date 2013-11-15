package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor.{ActorRef, Props, PoisonPill, ActorSystem}
import de.agilecoders.elasticsearch.logger.core.Lifecycle
import de.agilecoders.elasticsearch.logger.core.conf.Configuration

/**
 * Holds the `ActorSystem` instance.
 *
 * @author miha
 */
case class LogbackActorSystem(configuration: Configuration) extends Lifecycle[ActorSystem] {
    private[this] var _reaper: ActorRef = null
    lazy val instance: ActorSystem = ActorSystem.create(configuration.name, configuration.file)

    /**
     * starts up the actor system
     */
    def start(): ActorSystem = {
        _reaper = actorOf(Props(classOf[ShutdownReaper]), "reaper")

        instance
    }

    def actorOf(props: Props, name: String): ActorRef = instance.actorOf(props, name)

    def actorOf(props: Props): ActorRef = instance.actorOf(props)

    def publish(event: Event) = instance.eventStream.publish(event)

    def subscribe(subscriber: ActorRef, channels: Class[_]*) = {
        channels.foreach(instance.eventStream.subscribe(subscriber, _))
    }

    def reaper: ActorRef = _reaper

    /**
     * shutdown actor system
     */
    def stop(): Unit = {
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

    type Event = AnyRef
}