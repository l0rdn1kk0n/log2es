package de.agilecoders.elasticsearch.logger.log4j

import de.agilecoders.elasticsearch.logger.core.Log2esAppender
import org.apache.log4j.Appender
import org.apache.log4j.spi.LoggingEvent


/**
 * common base interface of java and scala appender
 *
 * @author miha
 */
trait ElasticSearchLog4jAppender extends Appender with Log2esAppender[LoggingEvent]



