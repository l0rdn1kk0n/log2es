package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor._
import com.twitter.ostrich.stats.Stats
import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import de.agilecoders.elasticsearch.logger.core.messages.{FlushQueue, Converted}
import de.agilecoders.elasticsearch.logger.core.store.Notifier
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentBuilder


/**
 * sends converted log messages to elasticsearch
 *
 * @author miha
 */
object IndexSender {
    def props() = Props(classOf[IndexSender])
}

/**
 * sends converted log messages to elasticsearch
 *
 * @author miha
 */
class IndexSender() extends Actor with RestartingSupervisor with ActorLogging with DefaultMessageHandler {
    private[this] lazy val configuration = newConfiguration()
    private[this] lazy val client = newStoreClient()

    override protected def onMessage = Stats.time("log2es.sender.onMessageTime") {
        case Converted(event: AnyRef) if event.isInstanceOf[XContentBuilder] => append(event.asInstanceOf[XContentBuilder])
        case event: XContentBuilder => append(event)
    }

    override protected def onFlushQueue(message: FlushQueue) = Stats.time("log2es.sender.onFlushQueueTime") {
        flush()
    }

    /**
     * appends a given log event to es bulk operation
     *
     * @param data log event as XContentBuilder
     */
    private[this] def append(data: XContentBuilder): Unit = {
        client.newEntry(data)

        Stats.incr("log2es.sender.received")
        Stats.addMetric("log2es.sender.queueSize", client.size)

        flushIfNecessary()
    }

    /**
     * flush all log messages if queue size was reached
     */
    private[this] def flushIfNecessary(): Unit = {
        if (client.size > configuration.queueSize) {
            flush()
        }
    }

    /**
     * flush all log messages if queue contains at least one element
     */
    private[this] def flush(): Unit = {
        if (client.size > 0) {
            log.debug(s"flush queue with size of ${client.size}")

            send()
        }
    }

    /**
     * sends data to store
     */
    private[this] def send(): Unit = {
        val queueSize = client.size

        Stats.time("log2es.sender.syncTime") {
            client.send(new Notifier[IndexRequest]() {
                def onFailure(e: Throwable, q: Iterable[IndexRequest]) = {
                    Stats.incr("log2es.sender.sentError")

                    // TODO miha: implement error handling
                }

                def onSuccess() = {
                    Stats.incr("log2es.sender.sent", queueSize)
                }
            })
        }
    }

    override def postRestart(reason: Throwable) = reason match {
        case _ => client.shutdown()
    }

    override def postStop() = {
        flush()

        client.shutdown()

        log.debug(s"shutting down sender: ${hashCode()}; current queue size: ${client.size};")

        super.postStop()
    }

    /**
     * creates a new store client instance
     */
    protected def newStoreClient() = log2es.dependencies.newStoreClient()

    /**
     * loads the configuration instance
     */
    protected def newConfiguration(): Configuration = log2es.dependencies.configuration
}