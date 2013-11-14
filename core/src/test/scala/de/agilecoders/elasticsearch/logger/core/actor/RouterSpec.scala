package de.agilecoders.elasticsearch.logger.logback.actor

import akka.actor.{ActorRef, PoisonPill, Props, ActorSystem}
import akka.routing.Broadcast
import akka.testkit.{ImplicitSender, TestKit}
import de.agilecoders.elasticsearch.logger.core.actor.Reaper.WatchMe
import de.agilecoders.elasticsearch.logger.core.messages.Action._
import org.scalatest.{Matchers, WordSpecLike}
import de.agilecoders.elasticsearch.logger.core.actor.Router

/**
 * Tests the converter actor
 *
 * @author miha
 */
class RouterSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers {
    def this() = this(ActorSystem.create())

    "A Router" must {

        "forward FlushQueue message to children" in {
            newRouter() ! flushQueue

            expectMsg(Broadcast(flushQueue))
        }

        "forward PoisonPill message to children" in {
            newRouter() ! PoisonPill.getInstance

            expectMsg(Broadcast(PoisonPill))
        }

        "answer to Alive with ImAlive" in {
            newRouter().tell(alive, testActor)

            expectMsg(imAlive)
        }

        "registers itself by sending WatchMe event" in {
            val router = newRouter()
            _system.eventStream.subscribe(testActor, classOf[WatchMe])

            router ! flushQueue

            expectMsg(WatchMe(router))
        }

    }

    private def newRouter(): ActorRef = {
        _system.actorOf(Props.apply(new Router() {
            override protected def newWorker() = testActor

            override protected def newErrorHandler() = testActor
        }))
    }
}
