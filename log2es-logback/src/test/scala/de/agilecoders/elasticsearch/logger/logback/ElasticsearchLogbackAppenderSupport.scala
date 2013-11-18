package de.agilecoders.elasticsearch.logger.logback

import akka.actor.ActorSystem
import ch.qos.logback.classic.LoggerContext
import de.agilecoders.elasticsearch.logger.core.ElasticsearchLogAppenderSupport

/**
 * helper class for all appender tests
 *
 * @author miha
 */
protected class ElasticsearchLogbackAppenderSupport(_system: ActorSystem) extends ElasticsearchLogAppenderSupport(_system) {

    lazy val appender: ActorBasedElasticSearchLogbackAppender = new ActorBasedElasticSearchLogbackAppender()

    override def start() {
        appender.setContext(new LoggerContext)
        appender.start()
    }

    override def stop() {
        appender.stop()
    }

    override def router() = appender.router

}
