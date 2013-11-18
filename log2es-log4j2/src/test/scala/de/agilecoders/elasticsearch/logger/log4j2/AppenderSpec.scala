package de.agilecoders.elasticsearch.logger.log4j2

import akka.actor.ActorSystem
import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import de.agilecoders.elasticsearch.logger.log4j2.mapper.LoggingEventToXContentMapper

object AppenderSpec {
    lazy val log2es = new Log2esContext(new Dependencies() {
        /**
         * creates a new mapper instance
         */
        def newMapper() = LoggingEventToXContentMapper(configuration)
    })
}

class AppenderSpec(_system: ActorSystem) extends ElasticsearchLog4j2AppenderSupport(_system) {
    def this() = this(AppenderSpec.log2es.dependencies.actorSystem.instance)

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
