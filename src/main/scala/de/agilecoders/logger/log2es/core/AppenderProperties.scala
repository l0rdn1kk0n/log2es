package de.agilecoders.logger.log2es.core

import java.util.concurrent.TimeUnit

import akka.util.Timeout

import scala.beans.BeanProperty
import scala.concurrent.duration.Duration

/**
 * Getter/Setter to configure elasticsearch logger
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait AppenderProperties {
  private val defaults = Configuration()

  def toConfiguration = Configuration(
    actorSystemName = getActorSystemName,
    flushQueueTime = Duration.apply(getFlushQueueTime),
    defaultTimeout = Timeout(Duration.apply(getDefaultTimeout).toSeconds, TimeUnit.SECONDS),
    clientType = getClientType,
    incomingBufferSize = getIncomingBufferSize,
    outgoingBulkSize = getOutgoingBulkSize,
    userAgent = getUserAgent,
    clusterName = getClusterName,
    fields = getFields.split(',').map(_.trim)
  )

  @BeanProperty
  var actorSystemName: String = defaults.actorSystemName

  @BeanProperty
  var clientType: String = defaults.clientType

  @BeanProperty
  var clusterName: String = defaults.clusterName

  @BeanProperty
  var userAgent: String = defaults.userAgent

  @BeanProperty
  var fields: String = defaults.fields.mkString("", ",", "")

  @BeanProperty
  var flushQueueTime: String = defaults.flushQueueTime.toString

  @BeanProperty
  var defaultTimeout: String = defaults.defaultTimeout.duration.toString()

  @BeanProperty
  var incomingBufferSize: Int = defaults.incomingBufferSize

  @BeanProperty
  var outgoingBulkSize: Int = defaults.outgoingBulkSize

}
