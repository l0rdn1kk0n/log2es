package de.agilecoders.logback.elasticsearch.actor

import akka.actor.{ActorLogging, Actor, ActorRef, Terminated}
import scala.collection.mutable.ArrayBuffer
import de.agilecoders.logback.elasticsearch.Log2esContext
import de.agilecoders.logback.elasticsearch.actor.Reaper.AllSoulsReaped

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
     * Derivations need to implement this method. It's the hook that's called when everything's dead
     */
    def allSoulsReaped(): Unit

    /**
     * Watch and check for termination
     */
    final def receive = {
        case WatchMe(ref) =>
            context.watch(ref)
            watched += ref

            log.debug("received new actor: " + ref.path)
        case Terminated(ref) =>
            watched -= ref
            log.debug("received terminated actor: " + ref.path)

            if (watched.isEmpty) {
                log.debug("all souls reaped")

                allSoulsReaped()
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