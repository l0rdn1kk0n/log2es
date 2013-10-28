package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import ch.qos.logback.classic.spi.ILoggingEvent
import de.agilecoders.logback.elasticsearch.{LogbackContext, IElasticSearchLogbackAppender}
import akka.routing.Broadcast

/**
 * router props.
 */
object Router {
    def props(appender: IElasticSearchLogbackAppender) = Props(classOf[Router], appender)
}

/**
 * The `Router` is responsible for delegating work to `Worker` actors and starting/stopping them
 *
 * @author miha
 */
class Router(appender: IElasticSearchLogbackAppender) extends Actor with ActorLogging with DefaultSupervisor {
    private[this] var worker: ActorRef = null
    private[this] var errorHandler: ActorRef = null

    override def receive = {
        case e: ILoggingEvent => worker ! e

        case p: PoisonPill => {
            worker ! Broadcast(p)
            context.system.stop(self)
        }
    }

    override def preStart() = {
        super.preStart()

        worker = Creator.newWorker(context)
        errorHandler = Creator.newErrorHandler(context, appender)
    }

    override def postStop() = {
        context.stop(worker)
        context.stop(errorHandler)

        super.postStop()

        LogbackContext.shutdown()
    }
}
