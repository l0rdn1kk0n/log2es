package de.agilecoders.logback.elasticsearch.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import de.agilecoders.logback.elasticsearch.Factory
import org.elasticsearch.common.xcontent.XContentBuilder
import org.scalatest.{Matchers, WordSpecLike}

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
