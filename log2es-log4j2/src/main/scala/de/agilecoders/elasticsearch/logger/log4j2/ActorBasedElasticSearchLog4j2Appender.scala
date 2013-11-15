package de.agilecoders.elasticsearch.logger.log4j2

import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.actor.{Names, Router}
import de.agilecoders.elasticsearch.logger.core.conf.Dependencies
import de.agilecoders.elasticsearch.logger.log4j2.mapper.LoggingEventToXContentMapper
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.{PluginElement, PluginAttribute, PluginFactory, Plugin}
import org.apache.logging.log4j.core.{Filter, LogEvent}

object ActorBasedElasticSearchLog4j2Appender {

    @PluginFactory
    def createAppender(@PluginAttribute("name") name: String,
                       @PluginElement("Filters") filter: Filter): ActorBasedElasticSearchLog4j2Appender = {
        new ActorBasedElasticSearchLog4j2Appender(log2esContext, name, Option(filter))
    }

    // TODO: context will be killed by appender, this should be done by factory
    private lazy val log2esContext: Log2esContext = {
        Log2esContext.create(new Dependencies {
            override def newMapper() = LoggingEventToXContentMapper(configuration)
        })
    }
}

/**
 * Special `org.apache.log4j.Appender` that is able to send all
 * log messages to an elasticsearch cluster. All log events will be handled
 * asynchronous.
 * <p/>
 * In order to optimize performance this appender deems events of level TRACE,
 * DEBUG and INFO as discardable
 *
 * @author miha
 */
@Plugin(name = "ActorBasedElasticSearchLog4j2Appender", category = "Core", elementType = "appender", printObject = true)
class ActorBasedElasticSearchLog4j2Appender(log2esContext: Log2esContext, name: String = "log2es", filter: Option[Filter] = None) extends AbstractAppender(name, filter.getOrElse(null), null) with ElasticSearchLog4j2Appender {
    protected[log4j2] lazy val router = log2esContext.dependencies.actorSystem.actorOf(Router.props(), Names.Router)
    protected[log4j2] lazy val discardableLevel = Level.toLevel(log2esContext.dependencies.configuration.discardable).intLevel()

    /**
     * sends given logging event to actor system
     *
     * @param eventObject log event to handle
     */
    override def append(eventObject: LogEvent): Unit = {
        if (!isStarted) {
            throw new IllegalStateException(s"AsyncAppender $name is not active")
        }

        router ! eventObject
    }

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    override def isDiscardable(event: LogEvent): Boolean = {
        event.getLevel.intLevel() <= discardableLevel
    }

    override def stop() {
        super.stop()

        log2esContext.shutdownAndAwaitTermination()
    }

    def addInfo(msg: String) = {}

    def addInfo(msg: String, ex: Throwable) = {}

    def addWarn(msg: String) = {}

    def addWarn(msg: String, ex: Throwable) = {}

    def addError(msg: String) = {}

    def addError(msg: String, ex: Throwable) = {}

    /**
     * This is where an appender accomplishes its work. Note that the argument
     * is of type Object.
     *
     * @param event logging event
     */
    def doAppend(event: LogEvent) = append(event)
}
