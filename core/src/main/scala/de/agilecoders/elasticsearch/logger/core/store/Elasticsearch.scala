package de.agilecoders.elasticsearch.logger.core.store

import akka.util.Timeout
import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import java.util.concurrent.TimeUnit
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.{ListenableActionFuture, ActionListener}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentBuilder
import scala.io.Source
import scalastic.elasticsearch.{ClientIndexer, Indexer}
import com.twitter.util.Duration

/**
 * TODO miha: document class purpose
 *
 * TODO miha: refactor and cleanup this file
 *
 * @author miha
 */
object Elasticsearch {
    private[this] var configuration: Option[Configuration] = None
    private[this] lazy val _client: Indexer = {
        configuration match {
            case Some(c: Configuration) => {
                val indexer: Indexer = new ClientIndexer(newTransportClient(c))
                indexer.start

                indexer.waitForNodes()
                indexer.waitTillActive()

                import org.elasticsearch.index.query.QueryBuilders._
                val count = try {
                    indexer.count(Seq(c.indexName), Seq(c.typeName), termQuery("_type", c.typeName)).getCount
                } catch {
                    case _: Throwable => 0
                }

                if (count <= 0) {
                    try {
                        indexer.createIndex(c.indexName)
                    } catch {
                        case e: Throwable => // TODO
                    }

                    if (c.initializeMapping) {
                        try {
                            indexer.putMapping(c.indexName, c.typeName, mapping)
                        } catch {
                            case e: Throwable => // TODO
                        }
                    }
                }

                indexer
            }
            case _ => throw new IllegalStateException()
        }
    }

    private[this] def newTransportClient(c: Configuration): TransportClient = {
        val settings = ImmutableSettings.settingsBuilder()
                       .put("node.data", false)
                       .put("node.client", true)
                       .put("client.transport.sniff", c.sniffHostnames)
                       .put("client.transport.ignore_cluster_name", c.clusterName == "")
                       .put("cluster.name", c.clusterName)
                       .build()

        val client = new TransportClient(settings)

        c.hosts.map(_.split(":")).foreach(host => {
            client.addTransportAddress(new InetSocketTransportAddress(host(0), Integer.parseInt(host(1))))
        })

        client
    }

    private[this] lazy val mapping: String = Source.fromURL(getClass.getResource("/mapping.json")).getLines()
                                             .mkString("\n")

    def newClient(configuration: Configuration): BufferedStore = {
        this.configuration = Some(configuration)

        Elasticsearch(_client, configuration)
    }
}


/**
 * Elasticsearch client that queues messages and sends them as bulk request to elasticsearch cluster.
 *
 * TODO miha: add retries for broken send requests.
 *
 * @author miha
 */
case class Elasticsearch(client: Indexer, configuration: Configuration) extends BufferedStore {
    private[this] lazy val queue = scala.collection.mutable.Queue[IndexRequest]()
    private[this] lazy val responses = scala.collection.mutable.ListBuffer[FutureResponse]()
    private[this] lazy val ttl:Long = {
        if(configuration.ttl > 0) {
            Duration.apply(configuration.ttl, TimeUnit.DAYS).inMilliseconds
        } else {
            0
        }
    }

    override def newEntry(data: XContentBuilder): IndexRequest = {
        val index = new IndexRequest(configuration.indexName, configuration.typeName)
        index.source(data)

        if (ttl > 0) {
            index.ttl(ttl)
        }

        queue += index

        index
    }

    /**
     * sends all queued messages to elasticsearch and calls the callback after it was finished
     *
     * @param q a message queue to send
     * @param notifier the callback
     * @return new future response
     */
    private[this] def sendInternal(q: Iterable[IndexRequest], notifier: Notifier[IndexRequest]): FutureResponse = {
        val response = client.bulk_send(q)
        val futureResponse: FutureResponse = new BulkFutureResponse(response) {
            protected val timeout = configuration.shutdownAwaitTimeout
        }

        response.addListener(new ActionListener[BulkResponse] {
            def onFailure(e: Throwable) = {
                responses -= futureResponse

                notifier.onFailure(e, q)
            }

            def onResponse(response: BulkResponse) = {
                responses -= futureResponse

                notifier.onSuccess()
            }
        })

        futureResponse
    }

    /**
     * block until all responses are arrived. Else there will be some missing log events.
     */
    private[this] def waitForResponses(): Unit = {
        responses.foreach(_.await())
        responses.clear()
    }

    override def send(notifier: Notifier[IndexRequest]) = {
        val q = queue

        if (q.size > 0) {
            val response = sendInternal(q, notifier)
            responses += response
        }

        queue.clear()
    }

    override def size = queue.size

    override def shutdown() {
        send(new Notifier[IndexRequest] {
            def onFailure(e: Throwable, q: Iterable[IndexRequest]) = queue.clear()

            def onSuccess() = queue.clear()
        })

        waitForResponses()
    }
}

/**
 * a special FutureResponse that wraps a ListenableActionFuture.
 */
protected[store] abstract class BulkFutureResponse(response: ListenableActionFuture[BulkResponse]) extends FutureResponse {

    override def await(timeout: Timeout) = {
        if (!response.isDone && !response.isCancelled) {
            try {
                get(timeout)
            } catch {
                case e: Throwable => // TODO: handle this error
            }
        }
    }

    override def get(timeout: Timeout) = response.get(timeout.duration.toSeconds, TimeUnit.SECONDS)
}

