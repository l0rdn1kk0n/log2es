package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{Props, Actor}
import akka.routing.{DefaultResizer, RoundRobinRouter}
import ch.qos.logback.classic.spi.ILoggingEvent
import com.twitter.ostrich.stats.Stats
import de.agilecoders.logback.elasticsearch.LogbackContext

/**
 * Converts an `ILoggingEvent` into a map
 *
 * @author miha
 */
object Converter {
    def props() = Props(classOf[Converter])
      .withRouter(RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 2, upperBound = 10))))
}

class Converter() extends Actor with DefaultSupervisor {
    private[this] lazy val transformer = LogbackContext.configuration.transformer

    def receive = {
        case event: ILoggingEvent => {
            sender ! convert(event)
        }
    }

    def convert(event: ILoggingEvent): AnyRef = {
        Stats.incr("log2es.converter.converted")

        Stats.time("log2es.converter.convertTime") {
                                                       transformer.toMap(event)
                                                   }
    }
}