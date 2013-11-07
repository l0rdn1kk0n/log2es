package de.agilecoders.logback.elasticsearch.store

/**
 * callback that is used to get informed when a response was successful or
 * got a failure.
 */
abstract class Notifier[R] {

    /**
     * A response handler.
     */
    def onSuccess()

    /**
     * A failure handler.
     *
     * @param e the exception that was thrown
     * @param queue the messages that were tried to sent
     */
    def onFailure(e: Throwable, queue: Iterable[R])

}
