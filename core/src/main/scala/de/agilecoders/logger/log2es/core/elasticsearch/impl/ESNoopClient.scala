package de.agilecoders.logger.log2es.core.elasticsearch.impl

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.elasticsearch.{ESClient, Response, StaticResponse}

import scala.concurrent.Future

/**
 * special noop client that can be used for testing
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class ESNoopClient(conf: Configuration) extends ESClient {

  override def shutdown(): Unit = {
    println("ESClient.shutdown")
  }

  override def status() = Future.successful("{\ncluster_name: \"elasticsearch_miha\",\nstatus: \"green\",\ntimed_out: false,\nnumber_of_nodes: 1,\nnumber_of_data_nodes: 1,\nactive_primary_shards: 0,\nactive_shards: 0,\nrelocating_shards: 0,\ninitializing_shards: 0,\nunassigned_shards: 0\n}")

  override def send(indexName: String, typeName: String, events: Seq[String]): Future[Response] = {
    println(s"ESClient.send($indexName, $typeName, ${events.length})")
    Future.successful(StaticResponse(200))
  }

  override def updateIndex(indexName: String, typeName: String, mapping: String) = {
    println(s"ESClient.putMapping($indexName, $typeName, $mapping)")
    Future.successful(StaticResponse(200))
  }

  override def start(): Unit = {
    println("ESClient.start")
  }

}
