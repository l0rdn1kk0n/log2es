package de.agilecoders.elasticsearch.logger.core.store

import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.transport.TransportRequest

object Store {

    /**
     * creates a new store client instance.
     *
     * @return new store client
     */
    def newClient[DataT, RequestT <: TransportRequest](configuration: Configuration): BufferedStore = Elasticsearch.newClient(configuration)
}

/**
 * Base trait for all store implementations. A Store is responsible for storing messages.
 */
trait Store[R] {

    /**
     * adds a new entry to the store. Some implementation uses a queue to improve performance, so make
     * sure to call send or shutdown at least once.
     *
     * @param entry new message to store
     * @return wrapped entry which was/will be added
     */
    def newEntry(entry: XContentBuilder): R

    /**
     * adds a new entry to the store. Some implementation uses a queue to improve performance, so make
     * sure to call send or shutdown at least once.
     *
     * @param entry new message to store
     * @return wrapped entry which was/will be added
     */
    def newEntry(entry: String): R

    /**
     * shutdown store client. This will also flush all queues and blocks until all requests are finished.
     */
    def shutdown()
}

trait BufferedStore extends Store[IndexRequest] {
    /**
     * sends all queued messages to the store.
     *
     * @param notifier Callback which is called when the messages were stored or an exception was thrown
     */
    def send(notifier: Notifier[IndexRequest])

    /**
     * @return current queue size
     */
    def size: Int
}