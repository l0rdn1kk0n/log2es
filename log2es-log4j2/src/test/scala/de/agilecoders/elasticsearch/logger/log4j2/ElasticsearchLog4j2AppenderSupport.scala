package de.agilecoders.elasticsearch.logger.log4j2

import akka.actor.ActorSystem
import de.agilecoders.elasticsearch.logger.core.ElasticsearchLogAppenderSupport

/**
 * helper class for all appender tests
 *
 * @author miha
 */
protected class ElasticsearchLog4j2AppenderSupport(_system: ActorSystem) extends ElasticsearchLogAppenderSupport(_system) {

    lazy val appender = ActorBasedElasticSearchLog4j2Appender.createAppender("log2esTest")

    override def start() {
        appender.start()
    }

    override def stop() {
        appender.stop()
    }

    override def router() = appender.router

}
