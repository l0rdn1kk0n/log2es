package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor._
import akka.routing.Broadcast
import de.agilecoders.elasticsearch.logger.core._
import de.agilecoders.elasticsearch.logger.core.actor.Reaper.WatchMe
import de.agilecoders.elasticsearch.logger.core.messages.{FlushQueue, Alive, CantSendEvent, Initialize}
import de.agilecoders.elasticsearch.logger.core.messages.Action._

/**
 * Router actor companion object
 */
object Router {
    /**
     * creates the actor props for the Router actor.
     */
    def props() = Props(classOf[Router])
}

/**
 * The `Router` is responsible for delegating work to `Worker` actors and starting/stopping them
 *
 * @author miha
 * @param appender the logback appender instance
 */
class Router() extends Actor with ActorLogging with DefaultSupervisor with ContextAware {
    private[this] lazy val worker: ActorRef = newWorker()
    private[this] lazy val errorHandler: ActorRef = newErrorHandler()
    private[this] lazy val mapper = log2es.dependencies.newMapper()

    /**
     * router is inactive by default
     */
    override def receive = inactive

    /**
     * receive handler when actor is dead/inactive
     */
    private[this] def inactive: Actor.Receive = {
        case i:Initialize => {
            log2es = i.context

            worker ! Broadcast(Initialize(log2es))
            errorHandler ! Initialize(log2es)

            context.become(active)
        }

        case _ => sender ! imDead
    }

    /**
     * receive handler when actor is active
     */
    private[this] def active: Actor.Receive = {
        case e: CantSendEvent => worker ! e.message
        case a: Alive => sender ! imAlive

        case Broadcast(flush: FlushQueue) => flushWorker(flush)
        case flush: FlushQueue => flushWorker(flush)

        case e: AnyRef if mapper.isSupported(e) => worker ! e

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
    protected def newWorker(): ActorRef = context.actorOf(Worker.props(log2es.dependencies.configuration.noOfWorkers), Names.Worker)

    /**
     * creates a new error handler actor reference
     *
     * @param appender the logback appender
     */
    protected def newErrorHandler(): ActorRef = context.actorOf(ErrorHandler.props(), Names.ErrorHandler)
}
