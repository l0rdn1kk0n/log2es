package de.agilecoders.elasticsearch.logger.logback.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.elasticsearch.common.xcontent.XContentBuilder
import org.scalatest.{Matchers, WordSpecLike}
import ch.qos.logback.classic.spi.LoggingEvent
import de.agilecoders.elasticsearch.logger.core.actor.Converter

/**
 * Tests the converter actor
 *
 * @author miha
 */
class ConverterSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with
Matchers {
    def this() = this(ActorSystem.create())

    "An Converter" must {

        "convert an event and forward it to sender " in {
            val actor = _system.actorOf(Converter.props(), "converter")

            actor.tell(new LoggingEvent(), testActor)

            expectMsgClass(classOf[XContentBuilder])
        }

    }
}
