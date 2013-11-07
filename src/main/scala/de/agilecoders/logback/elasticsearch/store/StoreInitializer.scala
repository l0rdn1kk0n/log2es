package de.agilecoders.logback.elasticsearch.store

/**
 * trait that represents a store initializer.
 */
trait StoreInitializer[T] {

    /**
     * establishes a connection to a store
     */
    def connect(): Boolean

    /**
     * disconnects from the store
     */
    def disconnect(): Unit

    /**
     * creates a new client that is able to interact with the store
     */
    def newClient(): T
}
