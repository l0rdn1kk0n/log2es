package de.agilecoders.elasticsearch.logger.core.store

import com.twitter.util.Duration
import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import java.util.concurrent.TimeUnit
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.Strings
import org.elasticsearch.common.xcontent.XContentBuilder
import scala.Some
import scala.collection.mutable.ArrayBuffer
import scalastic.elasticsearch.Indexer

/**
 * Special `Store` implementation that uses elasticsearch java client.
 *
 * TODO miha: refactor and cleanup this file
 *
 * @author miha
 */
object Elasticsearch {
    protected[store] val MAX_TTL = 1000000

    private[this] val _lock = new Object
    private[this] var _configuration: Option[Configuration] = None
    private[this] lazy val _client: Indexer = newIndexer()

    /**
     * @return new `Indexer` instance
     */
    private[this] def newIndexer(): Indexer = _configuration match {
        case Some(c: Configuration) => IndexerHolder(c).start()
        case _ => throw new IllegalArgumentException("there's no valid configuration")
    }

    /**
     * updates the configuration with given one and executes given function with `Indexer` as
     * parameter.
     *
     * @param configuration the configuration to use.
     */
    def withConfiguration(configuration: Configuration)(f: Indexer => BufferedStore): BufferedStore = {
        _lock.synchronized {
            if (!_configuration.isDefined) {
                _configuration = Some(configuration)
            }
        }

        f(_client)
    }

    /**
     * creates a new `Elasticsearch` client with given configuration.
     *
     * @param configuration the configuration to use.
     */
    def newClient(configuration: Configuration): BufferedStore = withConfiguration(configuration) {
        indexer =>
            Elasticsearch(indexer, configuration)
    }

    /**
     * shutdown `Indexer` and its transport client
     */
    def shutdown() = _client.stop()
}


/**
 * Elasticsearch client that queues messages and sends them as bulk request to elasticsearch cluster.
 *
 * TODO miha: add retries for broken send requests.
 *
 * @author miha
 */
case class Elasticsearch(client: Indexer, configuration: Configuration) extends BufferedStore {

    import Elasticsearch._

    private[this] lazy val error = new RuntimeException()
    private[this] lazy val queue = new ArrayBuffer[IndexRequest](configuration.queueSize)
    private[this] lazy val responses = scala.collection.mutable.ListBuffer[FutureResponse]()
    private[this] lazy val ttl: Long = {
        if (configuration.ttl > 0 && configuration.ttl < MAX_TTL) {
            Duration.apply(configuration.ttl, TimeUnit.DAYS).inMilliseconds
        } else {
            0
        }
    }

    /**
     * @return new `IndexRequest`
     */
    override def newEntry(data: XContentBuilder): IndexRequest = {
        val index = new IndexRequest(configuration.indexName, configuration.typeName)
                    .source(data)
                    .id(Strings.randomBase64UUID())
                    .opType(IndexRequest.OpType.CREATE)

        if (ttl > 0) {
            index.ttl(ttl)
        }

        queue += index

        index
    }

    /**
     * @return new `IndexRequest`
     */
    override def newEntry(data: String): IndexRequest = {
        val index = new IndexRequest(configuration.indexName, configuration.typeName)
          .source(data)
          .id(Strings.randomBase64UUID())
          .opType(IndexRequest.OpType.CREATE)

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

                response.hasFailures match {
                    case true => {
                        val ids = response.getItems.filter(_.isFailed).map(_.getId)

                        notifier.onFailure(error, q.filter(i => ids.contains(i.id())))
                    }
                    case _ => notifier.onSuccess()
                }
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

    override def send(notifier: Notifier[IndexRequest]) = if (queue.size > 0) {
        responses += sendInternal(queue, notifier)
        queue.clear()
    }

    override def size = queue.size

    override def shutdown() {
        send(ShutdownNotifier(queue))

        waitForResponses()
    }
}

case class ShutdownNotifier(queue: ArrayBuffer[IndexRequest]) extends Notifier[IndexRequest] {
    def onFailure(e: Throwable, q: Iterable[IndexRequest]) = queue.clear()

    def onSuccess() = queue.clear()
}

