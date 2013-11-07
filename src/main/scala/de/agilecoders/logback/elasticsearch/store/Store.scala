package de.agilecoders.logback.elasticsearch.store

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentBuilder

object Store {
    private[this] lazy val initializer: StoreInitializer[BufferedStore[XContentBuilder, IndexRequest]] = Elasticsearch

    /**
     * disconnects from store
     */
    def disconnect() = initializer.disconnect()

    /**
     * establishes a connection to the underlying store
     */
    def connect() = initializer.connect()

    /**
     * creates a new store client instance.
     *
     * @return new store client
     */
    def newClient(): BufferedStore[XContentBuilder, IndexRequest] = initializer.newClient()

}

/**
 * Base trait for all store implementations. A Store is responsible for storing messages.
 */
trait Store[T, R] {

    /**
     * adds a new entry to the store. Some implementation uses a queue to improve performance, so make
     * sure to call send or shutdown at least once.
     *
     * @param entry new message to store
     * @return wrapped entry which was/will be added
     */
    def newEntry(entry: T): R

    /**
     * shutdown store client. This will also flush all queues and blocks until all requests are finished.
     */
    def shutdown()
}

trait BufferedStore[T, R] extends Store[T, R] {
    /**
     * sends all queued messages to the store.
     *
     * @param notifier Callback which is called when the messages were stored or an exception was thrown
     */
    def send(notifier: Notifier[R])

    /**
     * @return current queue size
     */
    def size: Int
}