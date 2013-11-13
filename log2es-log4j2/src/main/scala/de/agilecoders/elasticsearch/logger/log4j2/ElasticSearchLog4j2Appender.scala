package de.agilecoders.elasticsearch.logger.log4j2

import de.agilecoders.elasticsearch.logger.core.Log2esAppender
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.LogEvent


/**
 * common base interface of java and scala appender
 *
 * @author miha
 */
trait ElasticSearchLog4j2Appender extends Appender with Log2esAppender[LogEvent]



