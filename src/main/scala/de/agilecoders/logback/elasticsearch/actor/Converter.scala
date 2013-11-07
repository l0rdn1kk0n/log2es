package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{PoisonPill, Props, Actor}
import akka.routing.RoundRobinRouter
import ch.qos.logback.classic.spi.ILoggingEvent
import com.twitter.ostrich.stats.Stats
import de.agilecoders.logback.elasticsearch.{FlushQueue, Log2esContext}

/**
 * Converts an `ILoggingEvent` into a map
 *
 * @author miha
 */
object Converter {
    def props() = Props(classOf[Converter]).withRouter(RoundRobinRouter(nrOfInstances = 10))
}

class Converter() extends Actor with DefaultSupervisor with DefaultMessageHandler {
    private[this] lazy val transformer = Log2esContext.configuration.transformer

    override protected def onMessage = {
        case event: ILoggingEvent => sender ! convert(event)
    }

    private def convert(event: ILoggingEvent): AnyRef = {
        Stats.incr("log2es.converter.converted")

        Stats.time("log2es.converter.convertTime") {
            transformer.map(event)
        }
    }

    protected def onFlushQueue(message: FlushQueue) = {
        // nothing to do here
    }

    protected def onPoisonPill(message: PoisonPill) = {
        // nothing to do here
    }
}