package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor.{ActorLogging, Props, Actor}
import akka.routing.RoundRobinRouter
import com.twitter.ostrich.stats.Stats
import de.agilecoders.elasticsearch.logger.core.messages.FlushQueue

/**
 * Converts an `ILoggingEvent` into a map
 *
 * @author miha
 */
object Converter {
    def props() = Props(classOf[Converter]).withRouter(RoundRobinRouter(nrOfInstances = 10))
}

case class Converter() extends Actor with DefaultSupervisor with ActorLogging with DefaultMessageHandler {
    private[this] lazy val mapper = newMapper()

    /**
     * reacts on all `event` messages and sends the converted result back to sender
     */
    override protected def onMessage = Stats.time("log2es.converter.onMessageTime") {
        case event: AnyRef if mapper.isSupported(event) => sender ! convert(event)
    }

    /**
     * converts given event to a `data` structure that is supported by elasticsearch
     *
     * @param event the event to convert
     */
    private[this] def convert(event: AnyRef): Any = Stats.time("log2es.converter.conversionTime") {
        mapper.map(event)
    }

    /**
     * onFlushQueue doesn't have to do anything because there's no queue
     *
     * @param message the flush queue message
     */
    protected override def onFlushQueue(message: FlushQueue) = Stats.time("log2es.converter.onFlushQueueTime") {
        // nothing to do here
    }

    /**
     * creates a new mapper instance
     */
    protected def newMapper() = log2es.dependencies.newMapper()
}