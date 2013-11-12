package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{ActorLogging, Actor, ActorRef, Terminated}
import de.agilecoders.logback.elasticsearch.Log2esContext
import de.agilecoders.logback.elasticsearch.actor.Reaper.AllSoulsReaped
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
abstract class Reaper extends Actor with ActorLogging {

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

case class ShutdownReaper() extends Reaper {
    /**
     * shutdown log2es context
     */
    def allSoulsReaped(): Unit = {
        context.system.eventStream.publish(AllSoulsReaped())

        Log2esContext.shutdown()
    }
}