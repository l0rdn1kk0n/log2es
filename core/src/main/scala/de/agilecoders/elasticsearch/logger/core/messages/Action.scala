package de.agilecoders.elasticsearch.logger.core.messages

import de.agilecoders.elasticsearch.logger.core.Log2esContext

/**
 * Holder object for static actions that can be shared
 */
object Action {
    lazy val flushQueue = FlushQueue()
    lazy val alive = Alive()
    lazy val imAlive = ImAlive()
    lazy val imDead = ImDead()
}

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

/**
 * initialize is send to all actors to initialize their state
 */
case class Initialize(context: Log2esContext) extends Action