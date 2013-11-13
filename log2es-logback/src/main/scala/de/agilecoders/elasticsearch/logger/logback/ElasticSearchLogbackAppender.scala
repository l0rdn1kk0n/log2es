package de.agilecoders.elasticsearch.logger.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import de.agilecoders.elasticsearch.logger.core.Log2esAppender


/**
 * common base interface of java and scala appender
 *
 * @author miha
 */
trait ElasticSearchLogbackAppender extends Appender[ILoggingEvent] with Log2esAppender[ILoggingEvent]



