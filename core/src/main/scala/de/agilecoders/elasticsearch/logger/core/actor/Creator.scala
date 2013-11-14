package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor.SupervisorStrategy.{Restart, Escalate, Resume}
import akka.actor._
import akka.routing.Broadcast
import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.messages.{FlushQueue, Initialize}
import scala.concurrent.duration._

/**
 * The `Creator` creates new `ActorRef` instances for all library actors.
 *
 * @author miha
 */
object Creator {

    def newRestartingSupervisor() = OneForOneStrategy(maxNrOfRetries = 10,
        withinTimeRange = 1 minute) {
                                        case _: RuntimeException => Restart
                                        case _: Exception => Escalate
                                    }

    def newSupervisor() = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
        case e: ActorInitializationException => Console.println(e.getMessage + ";\n" + e.toString); e.printStackTrace(Console.err); Escalate
        case _: RuntimeException => Resume
        case _: Exception => Escalate
    }

}

case object Names {
    lazy val Router = "router"
    lazy val Worker = "queued-worker"
    lazy val Converter = "converter"
    lazy val Sender = "sender"
    lazy val ErrorHandler = "error-handler"
    lazy val DeadLetter = "deadLetters"
}

trait DefaultSupervisor {
    this: Actor =>

    private[this] val _supervisor = Creator.newSupervisor()

    override def supervisorStrategy = _supervisor
}

trait RestartingSupervisor {
    this: Actor =>

    private[this] val _supervisor = Creator.newRestartingSupervisor()

    override def supervisorStrategy = _supervisor
}

trait DefaultMessageHandler extends ContextAware {
    this: Actor with ActorLogging =>

    final override def receive: Actor.Receive = {
        case Broadcast(i: Initialize) => initialize(i)
        case i: Initialize => initialize(i)
    }

    private def initialize(i: Initialize) {
        log2es = i.context
        context.become(activeReceiver)

        log.warning("initialized: " + self.path)
        onInitialized(log2es)
    }

    private def activeReceiver: Actor.Receive = {
        case message => onMessage.applyOrElse(message, onUnknownMessage)
    }

    protected def onMessage: Actor.Receive

    protected def onInitialized(log2es: Log2esContext): Unit = {}

    final protected def onUnknownMessage: Actor.Receive = {
        case Broadcast(flush: FlushQueue) => filter(flush, onFlushQueue)
        case flush: FlushQueue => filter(flush, onFlushQueue)

        case message => {
            unhandled(message)
        }
    }

    private def filter[A](m: A, x: (A) => Unit): Unit = {
        sender.path.name match {
            case Names.DeadLetter => // ignore deadLetter sender
            case _ => x(m)
        }
    }

    protected def onFlushQueue(message: FlushQueue): Unit

}

/**
 * interface that allows access to the log2es context
 */
trait ContextAware {
    protected var log2es: Log2esContext = null
}