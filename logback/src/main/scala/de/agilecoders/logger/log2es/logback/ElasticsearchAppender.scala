package de.agilecoders.logger.log2es.logback

import java.util.concurrent.atomic.AtomicBoolean

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.{ContextAwareBase, FilterAttachableImpl, FilterReply}
import de.agilecoders.logger.log2es.core.{AppenderProperties, Configuration, LogEventStream}

/**
 * elasticsearch asynchronous and remote logback appender
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class ElasticsearchAppender() extends ContextAwareBase with Appender[ILoggingEvent] with AppenderProperties with FilterSupport[ILoggingEvent] {

  private lazy val started = new AtomicBoolean(false)
  private lazy val conf = toConfiguration
  private lazy val logEventStream = LogEventStream(conf, LogbackEventMapper(conf))

  private var name = Configuration.defaults.name

  override def setName(name: String): Unit = this.name = name

  override def getName: String = name

  /**
   * @return true if this appender was started
   */
  override def isStarted = started.get()

  /**
   * start elasticsearch logger
   */
  override def start() = if (!isStarted) {
    started.set(true)

    logEventStream.start()
  }

  /**
   * shutdown elasticsearch logger
   */
  override def stop() = if (isStarted) {
    started.set(false)

    logEventStream.shutdown()
  }


  /**
   * append log event to elasticsearch
   *
   * @param event the log event
   */
  override def doAppend(event: ILoggingEvent) = if (isStarted && getFilterChainDecision(event) != FilterReply.DENY) {
    event.prepareForDeferredProcessing()

    logEventStream.enqueue(event)
  }

}

/**
 * event filter handling
 *
 * @tparam T the type of the event
 */
trait FilterSupport[T] {
  private lazy val fai = new FilterAttachableImpl[T]

  /**
   * Loop through the filters in the chain. As soon as a filter decides on
   * ACCEPT or DENY, then that value is returned. If all of the filters return
   * NEUTRAL, then NEUTRAL is returned.
   *
   * @param event the event to check
   * @return the filter decision
   */
  def getFilterChainDecision(event: T) = if (event != null) {
    fai.getFilterChainDecision(event)
  } else {
    FilterReply.DENY
  }

  /**
   * Get a copy of all the filters contained within this FilterAttachable
   * object.
   *
   * @return all attached filters as a list
   */
  def getCopyOfAttachedFiltersList = fai.getCopyOfAttachedFiltersList

  /**
   * Clear the filter chain
   */
  def clearAllFilters() = fai.clearAllFilters()

  /**
   * Add a filter to end of the filter list.
   *
   * @param newFilter filter to add to end of chain
   */
  def addFilter(newFilter: Filter[T]) = fai.addFilter(newFilter)
}