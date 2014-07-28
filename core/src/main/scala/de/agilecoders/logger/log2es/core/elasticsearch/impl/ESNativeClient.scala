package de.agilecoders.logger.log2es.core.elasticsearch.impl

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.elasticsearch.{ESClient, Response, StaticResponse}
import org.elasticsearch.action.admin.cluster.health.{ClusterHealthRequest, ClusterHealthResponse}
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.{ActionListener, ActionResponse}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.json4s._

import scala.concurrent.{Future, Promise}
import scala.util.Success

/**
 * elasticsearch client that uses the native java api
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
class ESNativeClient(conf: Configuration) extends ESClient {

  private lazy val client = {
    val settings = ImmutableSettings.settingsBuilder()
      .put("client.transport.sniff", true)
      .put("client.transport.ignore_cluster_name", true)
      .put("node.client", true)
      .put("node.data", false)
      .put("cluster.name", conf.clusterName)
      .build()

    new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress(conf.hostNameWithoutPort, conf.port))
  }

  /**
   * sends a bunch of events to elasticsearch
   *
   * @param indexName the index name to use
   * @param typeName the type name where this events should stored to
   * @param events the events to store
   * @return a future response
   */
  override def send(indexName: String, typeName: String, events: Seq[String]): Future[Response] = {
    val f = Promise[Response]()
    val bulk = client.prepareBulk()

    events.foreach(e => {
      bulk.add(client.prepareIndex(indexName, typeName).setSource(e))
    })

    bulk.setTimeout(TimeValue.timeValueSeconds(conf.defaultTimeout.duration.toSeconds))
    bulk.execute().addListener(Listener[BulkResponse](f))

    f.future
  }

  /**
   * updates/creates index and mapping
   *
   * @param indexName the index name to use
   * @param typeName the type name to update
   * @param configuration the configuration to write
   * @return a future response
   */
  override def updateIndex(indexName: String, typeName: String, configuration: String): Future[Response] = {
    import org.json4s.jackson.JsonMethods._
    val json: JValue = parse(configuration)

    val index = client.admin().indices().prepareExists(indexName).execute().actionGet()

    val f = Promise[Response]()

    if (!index.isExists) {
      val builder = client.admin().indices().prepareCreate(indexName)
      builder.setSettings(compact(json.\("settings")))
      builder.addMapping(typeName, compact(json.\("mappings")))
      builder.execute(Listener(f))
    } else {
      val builder = client.admin().indices().preparePutMapping(indexName)
      builder.setType(typeName)
      builder.setSource(compact(json.\("mappings")))
      builder.execute(Listener(f))
    }

    f.future
  }

  case class Listener[T <: ActionResponse](f: Promise[Response]) extends ActionListener[T] {
    override def onResponse(response: T): Unit = {
      Success(StaticResponse(200, ""))
    }

    override def onFailure(e: Throwable): Unit = f.failure(e)
  }

  /**
   * shutdown of elasticsearch client
   */
  override def shutdown(): Unit = {
    client.close()
  }

  /**
   * @return elasticsearch cluster status
   */
  override def status(): Future[String] = {
    val f = Promise[String]()
    client.admin().cluster().health(new ClusterHealthRequest(), new ActionListener[ClusterHealthResponse] {
      override def onFailure(e: Throwable): Unit = f.failure(e)

      override def onResponse(response: ClusterHealthResponse): Unit = f.complete(Success(response.getStatus.toString))
    })

    f.future
  }

  /**
   * starts the elasticsearch client
   */
  override def start(): Unit = {
    // nothing to do...
  }
}
