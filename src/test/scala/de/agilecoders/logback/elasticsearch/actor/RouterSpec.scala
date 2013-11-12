package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{ActorRef, PoisonPill, Props, ActorSystem}
import akka.routing.Broadcast
import akka.testkit.{ImplicitSender, TestKit}
import de.agilecoders.logback.elasticsearch._
import de.agilecoders.logback.elasticsearch.appender.ActorBasedElasticSearchLogbackAppender
import org.scalatest.{Matchers, WordSpecLike}
import de.agilecoders.logback.elasticsearch.actor.Reaper.WatchMe

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
        _system.actorOf(Props.apply(new Router(new ActorBasedElasticSearchLogbackAppender) {
            override protected def newWorker() = testActor

            override protected def newErrorHandler(appender: ElasticSearchLogbackAppender) = testActor
        }))
    }
}
