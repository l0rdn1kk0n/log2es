package de.agilecoders.logger.log2es.core.elasticsearch

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.elasticsearch.impl.{ESHttpClient, ESNoopClient}

import scala.concurrent.Future

/**
 * elasticsearch client factory
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object ESClient {

  /**
   * creates an elasticsearch client according to given client configuration
   *
   * @param conf the client configuration
   * @return new elasticsearch client
   */
  def create(conf: Configuration) = conf.clientType match {
    case "noop" => new ESNoopClient(conf)

    case "http" => new ESHttpClient(conf)

    case "native" => new ESNoopClient(conf)

    case clientType => throw new IllegalArgumentException(s"there's no such elasticsearch client: $clientType")
  }

}

/**
 * elasticsearch client interface
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait ESClient {

  /**
   * sends a bunch of events to elasticsearch
   *
   * @param indexName the index name to use
   * @param typeName the type name where this events should stored to
   * @param events the events to store
   * @return a future response
   */
  def send(indexName: String, typeName: String, events: Seq[String]): Future[Response]

  /**
   * updates/creates index and mapping
   *
   * @param indexName the index name to use
   * @param typeName the type name to update
   * @param configuration the configuration to write
   * @return a future response
   */
  def updateIndex(indexName: String, typeName: String, configuration: String): Future[Response]

  /**
   * @return elasticsearch cluster status
   */
  def status(): Future[String]

  /**
   * shutdown of elasticsearch client
   */
  def shutdown(): Unit

  /**
   * starts the elasticsearch client
   */
  def start(): Unit
}






