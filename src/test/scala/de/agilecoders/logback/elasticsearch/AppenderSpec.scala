package de.agilecoders.logback.elasticsearch

import akka.actor.ActorSystem
import de.agilecoders.logback.elasticsearch.actor.LogbackActorSystem

class AppenderSpec(_system: ActorSystem) extends ElasticsearchLogbackAppenderSupport(_system) {
    def this() = this(LogbackActorSystem.instance)

    "An Appender" must {

        "handle a lot of incoming events" in {
            (1 to 10000).par foreach (i => appender.append(Factory.newEvent()))

            waitForEmptyQueue()
        }

        "handle a small amount of messages and flush everything during shutdown" in {
            (1 to 1).par foreach (i => appender.append(Factory.newEvent()))

            waitForEmptyQueue()
        }

    }


}
