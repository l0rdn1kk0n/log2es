package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{PoisonPill, Props, Actor}
import akka.routing.RoundRobinRouter
import ch.qos.logback.classic.spi.ILoggingEvent
import com.twitter.ostrich.stats.Stats
import de.agilecoders.logback.elasticsearch.FlushQueue
import de.agilecoders.logback.elasticsearch.conf.DependencyHolder
import de.agilecoders.logback.elasticsearch.mapper.LoggingEventMapper
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Converts an `ILoggingEvent` into a map
 *
 * @author miha
 */
object Converter {
    def props() = Props(classOf[Converter]).withRouter(RoundRobinRouter(nrOfInstances = 10))
}

case class Converter() extends Actor with DefaultSupervisor with DefaultMessageHandler with DependencyHolder {
    private[this] lazy val mapper: LoggingEventMapper[XContentBuilder] = newMapper()

    override protected def onMessage = {
        case event: ILoggingEvent => sender ! convert(event)
    }

    private[this] def convert(event: ILoggingEvent): AnyRef = Stats.time("log2es.converter.convertTime") {
        mapper.map(event)
    }

    protected override def onFlushQueue(message: FlushQueue) = {
        // nothing to do here
    }

    /**
     * loads the mapper instance
     */
    protected def newMapper(): LoggingEventMapper[XContentBuilder] = dependencies.mapper
}