package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import akka.routing.Broadcast
import ch.qos.logback.classic.spi.ILoggingEvent
import de.agilecoders.logback.elasticsearch._

/**
 * router props.
 */
object Router {
    def props(appender: ElasticSearchLogbackAppender) = Props(classOf[Router], appender)
}

/**
 * The `Router` is responsible for delegating work to `Worker` actors and starting/stopping them
 *
 * @author miha
 */
class Router(appender: ElasticSearchLogbackAppender) extends Actor with ActorLogging with DefaultSupervisor {
    private[this] var worker: ActorRef = null
    private[this] var errorHandler: ActorRef = null

    /**
     * router is inactive by default
     */
    override def receive = inactive

    /**
     * receive handler when actor is dead/inactive
     */
    def inactive: Actor.Receive = {
        case p: PoisonPill => ignore()
        case PoisonPill => ignore()
        case Broadcast(PoisonPill) => ignore()
        case Broadcast(p: PoisonPill) => ignore()
        case _ => {
            sender ! imDead
        }
    }

    private[this] def ignore(): Unit = {
        // ignore because we're already dead
        log.info("got poison pill message but i'm dead")
    }

    /**
     * receive handler when actor is active
     */
    def active: Actor.Receive = {
        case e: ILoggingEvent => worker ! e
        case e: CantSendEvent => worker ! e.message

        case Alive => {
            sender ! imAlive
        }
        case a: Alive => {
            sender ! imAlive
        }

        case Broadcast(p: PoisonPill) => becomeInactive(p)
        case Broadcast(PoisonPill) => becomeInactive(PoisonPill.getInstance)
        case p: PoisonPill => becomeInactive(p)
        case PoisonPill => becomeInactive(PoisonPill.getInstance)

        case Broadcast(flush: FlushQueue) => flushWorker(flush)
        case flush: FlushQueue => flushWorker(flush)
        case FlushQueue => flushWorker(FlushQueue())

        case unknown => {
            log.warning(unknown.toString)
        }
    }

    private[this] def becomeInactive(p: PoisonPill): Unit = {
        worker ! Broadcast(p)
        context.become(inactive)
    }

    private[this] def flushWorker(flush: FlushQueue): Unit = {
        log.debug(s"got flush queue event: ${sender.path}")

        worker ! Broadcast(flush)
    }

    override def preStart() = {
        super.preStart()

        worker = Creator.newWorker(context)
        errorHandler = Creator.newErrorHandler(context, appender)

        context.become(active)

        Log2esContext.watchMe(self)
    }

    override def postStop() = {
        context.become(inactive)

        context.stop(worker)
        context.stop(errorHandler)

        super.postStop()
    }
}
