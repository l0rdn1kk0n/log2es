package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import com.twitter.ostrich.stats.Stats
import de.agilecoders.logback.elasticsearch.Error
import de.agilecoders.logback.elasticsearch.FlushQueue
import de.agilecoders.logback.elasticsearch.{JestElasticsearchClientFactory, LogbackContext}
import io.searchbox.client.{JestClient, JestResult, JestResultHandler}
import io.searchbox.core.{Index, Bulk}
import java.util.concurrent.atomic.AtomicInteger

/**
 * sends converted log messages to elasticsearch
 *
 * @author miha
 */
object IndexSender {
    lazy val messagesSent = new AtomicInteger(0)
    lazy val registeredClients = new AtomicInteger(0)

    def props() = Props(classOf[IndexSender])

    //.withRouter(RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 2))))

    protected def client(): JestClient = {
        JestElasticsearchClientFactory.newInstance(LogbackContext.configuration)
    }

}

class IndexSender() extends Actor with RestartingSupervisor with ActorLogging {
    private[this] val configuration = LogbackContext.configuration
    private[this] val client = IndexSender.client()
    private[this] var bulk: Bulk.Builder = newBuilder()
    private[this] var count: Int = 0
    private[this] var received: Int = 0

    def receive = {
        case event: PoisonPill => flush()
        case event: FlushQueue => flush()
        case event: AnyRef => append(event)
    }

    private[this] def append(data: AnyRef) = {
        bulk.addAction(newIndex(data))
        count += 1
        received += 1

        Stats.incr("log2es.sender.received")
        Stats.addMetric("log2es.sender.queueSize", count)

        flushIfNecessary()
    }

    /**
     * flush all log messages if queue size was reached
     */
    private[this] def flushIfNecessary() {
        if (count > configuration.queueSize) {
            flush()
        }
    }

    /**
     * flush all log messages if queue contains at least one element
     */
    private[this] def flush() {
        if (count > 0) {
            send(bulk.build(), count)

            bulk = newBuilder()
            count = 0
        }
    }

    /**
     * creates a new index action
     *
     * @param data the data to index
     * @return new index action
     */
    private[this] def newIndex(data: AnyRef): Index = new Index.Builder(data)
      .index(configuration.indexName)
      .`type`(configuration.typeName)
      .build()

    /**
     * sends data to elasticsearch
     *
     * @param bulk the bulk action to execute
     */
    private[this] def send(bulk: Bulk, count: Int, retries: Int = 0): Unit = {
        if (configuration.useAsyncHttp) {
            client.executeAsync(bulk, new JestResultHandler[JestResult] {
                def completed(result: JestResult) = {
                    result.isSucceeded match {
                        case false => {
                            if (isAlive) {
                                retry(new RuntimeException(result.getErrorMessage))
                            }

                            log.warning(s"can't sent $count messages")
                        }
                        case _ => {
                            if (isAlive) {
                                log.debug(s"send $count messages to elasticsearch")
                            }

                            log.debug(s"sent $count messages")
                            IndexSender.messagesSent.addAndGet(count)
                            Stats.incr("log2es.sender.sent", count)
                        }
                    }
                }

                def failed(ex: Exception) = {
                    if (isAlive) {
                        retry(ex)
                    }

                    log.error(s"can't sent $count messages; failed with ex: ${ex.getMessage}", ex)
                }

                private def retry(ex: Exception) {
                    if (retries < 3) {
                        send(bulk, count, retries + 1)
                    } else {
                        context.system.eventStream.publish(Error(ex))
                    }
                }

                private[this] def isAlive: Boolean = {
                    context != null && context.system != null && !context.system.isTerminated
                }
            })
        } else {
            Stats.time("log2es.sender.syncTime") {
                                                     client.execute(bulk)
                                                 }

            IndexSender.messagesSent.addAndGet(count)
            Stats.incr("log2es.sender.sent", count)
        }
    }

    /**
     * @return new bulk action builder instance
     */
    private[this] def newBuilder(): Bulk.Builder = new Bulk.Builder()
      .defaultIndex(configuration.indexName)
      .defaultType(configuration.typeName)

    override def postRestart(reason: Throwable) = reason match {
        case e: RuntimeException => {
            client.shutdownClient()

            val tempClient = IndexSender.client()
            tempClient.execute(bulk.build())
            tempClient.shutdownClient()
        }
        case _ => // TODO miha: ???
    }

    override def postStop() = {
        flush()

        log.info(s"shutting down sender: ${hashCode()}; current queue size: $count; received: $received; overall " +
                 s"messages: ${IndexSender.messagesSent.get()}")

        super.postStop()
    }

    override def preStart() = {
        super.preStart()

        log.info(s"starting sender: ${hashCode()}")

        count = 0
    }
}