package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import com.twitter.ostrich.stats.Stats
import de.agilecoders.logback.elasticsearch.FlushQueue
import de.agilecoders.logback.elasticsearch.Log2esContext
import de.agilecoders.logback.elasticsearch.store.{Store, Notifier}
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
    private[this] lazy val configuration = Log2esContext.configuration
    private[this] lazy val client = Store.newClient()
    private[this] var received: Int = 0

    override protected def onMessage = {
        case event: XContentBuilder => append(event)
    }

    override protected def onFlushQueue(message: FlushQueue) = flush()

    override protected def onPoisonPill(message: PoisonPill) = client.shutdown()

    private[this] def append(data: XContentBuilder) = {
        client.newEntry(data)
        received += 1

        Stats.incr("log2es.sender.received")
        Stats.addMetric("log2es.sender.queueSize", client.size)

        flushIfNecessary()
    }

    /**
     * flush all log messages if queue size was reached
     */
    private[this] def flushIfNecessary() {
        if (client.size > configuration.queueSize) {
            flush()
        }
    }

    /**
     * flush all log messages if queue contains at least one element
     */
    private[this] def flush() {
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

                    // TODO add error handling...
                }

                def onSuccess() = {
                    Stats.incr("log2es.sender.sent", queueSize)
                }
            })
        }
    }

    override def postRestart(reason: Throwable) = reason match {
        case e: RuntimeException => {
            client.shutdown()
            // TODO
        }
        case _ => // TODO miha: ???
    }

    override def postStop() = {
        flush()

        client.shutdown()

        log.debug(s"shutting down sender: ${hashCode()}; current queue size: ${client.size}; received: $received;")

        super.postStop()
    }
}