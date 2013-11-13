package de.agilecoders.elasticsearch.logger.core

/**
 * `Lifecycle` trait provides methods to start and stop an implementing
 * class.
 *
 * @author miha
 */
trait Lifecycle[T] {

    /**
     * start is called to startup the class that implements this interface
     *
     * @return T
     */
    def start(): T

    /**
     * stops the implementing class
     */
    def stop(): Unit

}
