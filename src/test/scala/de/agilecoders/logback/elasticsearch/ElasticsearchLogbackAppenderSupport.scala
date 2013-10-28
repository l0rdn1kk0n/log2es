package de.agilecoders.logback.elasticsearch

import akka.actor.ActorSystem
import akka.testkit.{TestKit, ImplicitSender}
import ch.qos.logback.classic.spi.{LoggingEvent, ILoggingEvent}
import ch.qos.logback.classic.{Level, LoggerContext}
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * helper class for all appender tests
 *
 * @author miha
 */
protected class ElasticsearchLogbackAppenderSupport(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with
WordSpecLike with Matchers with BeforeAndAfterAll {
    lazy val appender = new ElasticSearchLogbackAppender

    override protected def afterAll(): Unit = {
        appender.stop()
    }

    override protected def beforeAll(): Unit = {
        appender.setContext(new LoggerContext)
        appender.start()
    }

    protected def newEvent(): ILoggingEvent = newEvent(scala.math.random)

    protected def newEvent(number: Double): ILoggingEvent = {
        val event = new LoggingEvent()

        event.setMessage("message" + number)
        event.setThreadName("thread" + number)
        event.setLevel(Level.WARN)
        event.setLoggerName("logger" + number)
        event.setTimeStamp(DateTime.now().getMillis)

        event
    }
}
