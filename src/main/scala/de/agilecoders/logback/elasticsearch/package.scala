package de.agilecoders.logback

import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * package object
 *
 * @author miha
 */
package object elasticsearch {

    implicit val executor = Log2esContext.system.dispatcher

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
     * Message that holds converted content from converter
     */
    case class Converted(content: AnyRef) extends Action

    /**
     * action which will be sent to flush all queues on all workers
     */
    case class FlushQueue() extends Action

    /**
     * action which is send to actors which asked for
     */
    case class Alive() extends Action

    /**
     * action which is send to actors which asked for
     */
    case class ImDead() extends Action

    /**
     * response to Alive
     */
    case class ImAlive() extends Action

    lazy val flushQueue = FlushQueue
    lazy val alive = Alive
    lazy val imAlive = ImAlive
    lazy val imDead = ImDead

}
