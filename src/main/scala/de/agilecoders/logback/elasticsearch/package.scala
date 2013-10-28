package de.agilecoders.logback

import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * package object
 *
 * @author miha
 */
package object elasticsearch {

    implicit val executor = LogbackContext.system.dispatcher

    /**
     * a base feedback message
     */
    sealed trait Feedback

    /**
     * message that is thrown if a log message can't be sent
     */
    case class CantSendEvent(message: ILoggingEvent) extends Feedback

    /**
     * an error message is thrown if there is an exception which wasn't handled
     */
    case class Error(throwable: Throwable) extends Feedback

    /**
     * base action message
     */
    sealed trait Action

    /**
     * action which will be sent to flush all queues on all workers
     */
    case class FlushQueue() extends Action

    lazy val flushQueue = FlushQueue

}
