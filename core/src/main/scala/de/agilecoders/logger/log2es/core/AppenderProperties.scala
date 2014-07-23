package de.agilecoders.logger.log2es.core

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import de.agilecoders.logger.log2es.core.mapper.Fields

import scala.beans.BeanProperty
import scala.concurrent.duration.Duration

/**
 * Getter/Setter to configure elasticsearch logger
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait AppenderProperties {
  private val defaults = Configuration()

  /**
   * @return all appender properties as immutable configuration class
   */
  def toConfiguration = Configuration(
    host = getHost,
    actorSystemName = getActorSystemName,
    flushQueueTime = Duration.apply(getFlushQueueTime),
    defaultTimeout = Timeout(Duration.apply(getDefaultTimeout).toSeconds, TimeUnit.SECONDS),
    clientType = getClientType,
    incomingBufferSize = getIncomingBufferSize,
    outgoingBulkSize = getOutgoingBulkSize,
    userAgent = getUserAgent,
    clusterName = getClusterName,
    ttl = getTtl,
    esConfigurationFile = getEsConfigurationFile,
    name = getName,
    serviceName = getServiceName,
    gzip = getGzip,
    typeName = getTypeName,
    indexName = getIndexName,
    hostName = getHostName,
    typeNameUpdateInterval = Duration.apply(getTypeNameUpdateInterval),
    fields = Fields.parse(getFields)
  )

  @BeanProperty
  var name: String = defaults.name

  @BeanProperty
  var typeNameUpdateInterval: String = defaults.typeNameUpdateInterval.toString

  @BeanProperty
  var actorSystemName: String = defaults.actorSystemName

  @BeanProperty
  var serviceName: String = defaults.serviceName

  @BeanProperty
  var hostName: String = defaults.hostName

  @BeanProperty
  var indexName: String = defaults.indexName

  @BeanProperty
  var typeName: String = defaults.typeName

  @BeanProperty
  var host: String = defaults.host

  @BeanProperty
  var clientType: String = defaults.clientType

  @BeanProperty
  var clusterName: String = defaults.clusterName

  @BeanProperty
  var userAgent: String = defaults.userAgent

  @BeanProperty
  var ttl: String = defaults.ttl

  @BeanProperty
  var updateMapping: Boolean = defaults.updateMapping

  @BeanProperty
  var gzip: Boolean = defaults.gzip

  @BeanProperty
  var esConfigurationFile: String = defaults.esConfigurationFile

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
