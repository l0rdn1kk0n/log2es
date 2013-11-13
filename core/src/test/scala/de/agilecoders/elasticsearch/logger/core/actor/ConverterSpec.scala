package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import de.agilecoders.logback.elasticsearch.actor.Converter
import org.elasticsearch.common.xcontent.XContentBuilder
import org.scalatest.{Matchers, WordSpecLike}
import de.agilecoders.elasticsearch.logger.core.Factory

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

            actor.tell(Factory.newEvent(), testActor)

            expectMsgClass(classOf[XContentBuilder])
        }

    }
}
