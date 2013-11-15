package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor.{ActorLogging, Actor, ActorRef, Terminated}
import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.actor.Reaper.AllSoulsReaped
import de.agilecoders.elasticsearch.logger.core.messages.Initialize
import scala.collection.mutable.ArrayBuffer

object Reaper {

    /**
     * Used by others to register an Actor for watching
     *
     * @param ref the actor to watch
     */
    case class WatchMe(ref: ActorRef)

    /**
     * Used by reaper to publish "AllSoulsReaped" event.
     */
    case class AllSoulsReaped()

}

/**
 * The reaper is responsible for watching all actors and if all of them are dead
 * shutting down the ActorSystem.
 */
abstract class Reaper extends Actor with ActorLogging with ContextAware {

    import Reaper._

    // Keep track of what we're watching
    private[this] lazy val watched = ArrayBuffer.empty[ActorRef]

    /**
     * Derivations need to implement this method. It's the hook that's called when everything is dead
     */
    def allSoulsReaped(): Unit

    /**
     * Watch and check for termination
     */
    final def receive = {
        case Initialize(c: Log2esContext) => log2es = c

        case WatchMe(ref) => {
            context.watch(ref)
            watched += ref
        }

        case Terminated(ref) => {
            watched -= ref

            if (watched.isEmpty) {
                allSoulsReaped()
            }
        }
    }

    override def preStart() = {
        super.preStart()

        context.system.eventStream.subscribe(self, classOf[WatchMe])
    }
}

/**
 * special reaper that stops the log2es context
 */
case class ShutdownReaper() extends Reaper {
    /**
     * shutdown log2es context
     */
    def allSoulsReaped(): Unit = {
        context.system.eventStream.publish(AllSoulsReaped())

        log2es.stop()
    }
}