package de.agilecoders.logback.elasticsearch.actor

import akka.actor.SupervisorStrategy.{Restart, Escalate, Resume}
import akka.actor._
import akka.routing.Broadcast
import de.agilecoders.logback.elasticsearch.{FlushQueue, Log2esContext, ElasticSearchLogbackAppender}
import scala.concurrent.duration._
import de.agilecoders.logback.elasticsearch.actor.Creator.Names

/**
 * The `Creator` creates new `ActorRef` instances for all library actors.
 *
 * @author miha
 */
object Creator {

    private[this] lazy val system = Log2esContext.system

    def newRouter(appender: ElasticSearchLogbackAppender): ActorRef = system.actorOf(Router.props(appender), Names.Router)

    def newWorker(context: ActorContext): ActorRef = context.actorOf(enhance(Worker.props()), Names.Worker)

    def newIndexSender(context: ActorContext): ActorRef = context.actorOf(IndexSender.props(), Names.Sender)

    def newErrorHandler(context: ActorContext, appender: ElasticSearchLogbackAppender): ActorRef = {
        val actor = context.actorOf(ErrorHandler.props(appender), Names.ErrorHandler)
        system.eventStream.subscribe(actor, classOf[DeadLetter])
        system.eventStream.subscribe(actor, classOf[PoisonPill])
        system.eventStream.subscribe(actor, classOf[UnhandledMessage])

        actor
    }

    def newConverter(context: ActorContext): ActorRef = context.actorOf(Converter.props(), Names.Converter)

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

    private[this] def enhance(props: Props): Props = {
        props
        .withMailbox("unbounded")
        .withDispatcher("log2es-dispatcher")
    }

    case object Names {
        lazy val Router = "router"
        lazy val Worker = "queued-worker"
        lazy val Converter = "converter"
        lazy val Sender = "sender"
        lazy val ErrorHandler = "error-handler"
        lazy val DeadLetter = "deadLetters"
    }
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

trait DefaultMessageHandler {
    this: Actor =>

    final override def receive: Actor.Receive = {
        case message => onMessage.applyOrElse(message, onUnknownMessage)
    }

    protected def onMessage: Actor.Receive

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