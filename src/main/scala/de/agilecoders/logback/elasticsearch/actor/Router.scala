package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import akka.routing.Broadcast
import ch.qos.logback.classic.spi.ILoggingEvent
import de.agilecoders.logback.elasticsearch._
import de.agilecoders.logback.elasticsearch.actor.Reaper.WatchMe
import de.agilecoders.logback.elasticsearch.conf.DependencyHolder

/**
 * Router actor companion object
 */
object Router {
    /**
     * creates the actor props for the Router actor.
     *
     * @param appender the logback appender instance
     */
    def props(appender: ElasticSearchLogbackAppender) = Props(classOf[Router], appender)
}

/**
 * The `Router` is responsible for delegating work to `Worker` actors and starting/stopping them
 *
 * @author miha
 * @param appender the logback appender instance
 */
class Router(appender: ElasticSearchLogbackAppender) extends Actor with ActorLogging with DefaultSupervisor with DependencyHolder {
    private[this] val worker: ActorRef = newWorker()
    private[this] val errorHandler: ActorRef = newErrorHandler(appender)

    /**
     * router is inactive by default
     */
    override def receive = inactive

    /**
     * receive handler when actor is dead/inactive
     */
    private[this] def inactive: Actor.Receive = {
        case _ => sender ! imDead
    }

    /**
     * receive handler when actor is active
     */
    private[this] def active: Actor.Receive = {
        case e: ILoggingEvent => worker ! e
        case e: CantSendEvent => worker ! e.message

        case a: Alive => sender ! imAlive

        case Broadcast(flush: FlushQueue) => flushWorker(flush)
        case flush: FlushQueue => flushWorker(flush)

        case unknown => {
            log.warning(unknown.toString)
        }
    }

    /**
     * changes the default receive method to the inactive handler which
     * does nothing except answering with `imDead`
     *
     * @param p the poison pill message to send to children
     */
    private[this] def becomeInactive(p: PoisonPill): Unit = {
        worker ! Broadcast(p)
        context.become(inactive)
    }

    /**
     * flushes the queues of all children
     *
     * @param flush the flush queue message to propagate
     */
    private[this] def flushWorker(flush: FlushQueue): Unit = worker ! Broadcast(flush)


    /**
     * preStart handler that activates receive method and adds
     * this actor reference to the reaper watch list.
     */
    override def preStart() = {
        super.preStart()

        context.system.eventStream.publish(WatchMe(self))
        context.become(active)
    }

    /**
     * postStop handler that sends poison pill to all children, stops them and
     * deactivates the receive method.
     */
    override def postStop() = {
        becomeInactive(PoisonPill.getInstance)

        context.stop(worker)
        context.stop(errorHandler)

        super.postStop()
    }

    /**
     * creates a new worker actor reference
     */
    protected def newWorker(): ActorRef = dependencies.newWorker(context)

    /**
     * creates a new error handler actor reference
     *
     * @param appender the logback appender
     */
    protected def newErrorHandler(appender: ElasticSearchLogbackAppender): ActorRef = dependencies.newErrorHandler(context, appender)
}
