package de.agilecoders.logback.elasticsearch

import akka.actor.ActorSystem
import de.agilecoders.logback.elasticsearch.actor.LogbackActorSystem
import com.twitter.ostrich.stats.Stats

class AppenderSpec(_system: ActorSystem) extends ElasticsearchLogbackAppenderSupport(_system) {
    def this() = this(LogbackActorSystem.instance)

    "An Appender" must {

        "handle a lot of incoming events" in {
            //(1 to 1000000) foreach (i => appender.append(newEvent()))
            //Thread.sleep(20000)
        }

        "handle a small amount of messages and flush everything during shutdown" in {
            (1 to 9999).par foreach (i => appender.append(newEvent()))
            Thread.sleep(5000)
        }

    }


}
