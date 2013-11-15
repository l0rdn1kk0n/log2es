package de.agilecoders.elasticsearch.logger.core

/**
 * TODO
 */
trait Log2esAppender[T] extends EventAppender[T] with StateAware with Discardable[T] {}

/**
 * TODO
 */
trait EventAppender[T] {
    /**
     * This is where an appender accomplishes its work. Note that the argument
     * is of type Object.
     *
     * @param event logging event
     */
    def doAppend(event: T)
}

/**
 * TODO
 */
trait Discardable[T] {
    /**
      * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
      *
      * @param event the logging event
      * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
      */
    def isDiscardable(event: T): Boolean
}

/**
 * TODO
 */
trait StateAware {
    def addInfo(msg: String)

    def addInfo(msg: String, ex: Throwable)

    def addWarn(msg: String)

    def addWarn(msg: String, ex: Throwable)

    def addError(msg: String)

    def addError(msg: String, ex: Throwable)
}