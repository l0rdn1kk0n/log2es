package de.agilecoders.logback.elasticsearch.actor

import akka.actor.SupervisorStrategy.{Restart, Escalate, Resume}
import akka.actor._
import akka.routing.Broadcast
import de.agilecoders.logback.elasticsearch.{FlushQueue, Log2esContext, ElasticSearchLogbackAppender}
import scala.concurrent.duration._

/**
 * The `Creator` creates new `ActorRef` instances for all library actors.
 *
 * @author miha
 */
object Creator {

    private[this] lazy val system = Log2esContext.system

    def newRouter(appender: ElasticSearchLogbackAppender): ActorRef = system.actorOf(Router.props(appender), "router")

    def newWorker(context: ActorContext): ActorRef = context.actorOf(enhance(Worker.props()), "queued-worker")

    def newIndexSender(context: ActorContext): ActorRef = context.actorOf(IndexSender.props(), "sender")

    def newErrorHandler(context: ActorContext, appender: ElasticSearchLogbackAppender): ActorRef = {
        val actor = context.actorOf(ErrorHandler.props(appender), "error-handler")
        system.eventStream.subscribe(actor, classOf[DeadLetter])
        system.eventStream.subscribe(actor, classOf[PoisonPill])
        system.eventStream.subscribe(actor, classOf[UnhandledMessage])

        actor
    }

    def newConverter(context: ActorContext): ActorRef = context.actorOf(Converter.props(), "converter")

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
        case Broadcast(p: PoisonPill) => filter(p, onPoisonPill)
        case Broadcast(PoisonPill) => filter(PoisonPill.getInstance, onPoisonPill)
        case p: PoisonPill => filter(p, onPoisonPill)
        case PoisonPill => filter(PoisonPill.getInstance, onPoisonPill)

        case Broadcast(flush: FlushQueue) => filter(flush, onFlushQueue)
        case Broadcast(FlushQueue) => filter(FlushQueue(), onFlushQueue)
        case flush: FlushQueue => filter(flush, onFlushQueue)
        case FlushQueue => filter(FlushQueue(), onFlushQueue)

        case message => {
            unhandled(message)
        }
    }

    private def filter[A](m:A, x: (A) => Unit):Unit = {
        sender.path.name match {
            case "deadLetters" => // ignore deadLetter sender
            case _ => x(m)
        }
    }

    protected def onFlushQueue(message: FlushQueue): Unit

    protected def onPoisonPill(message: PoisonPill): Unit

    // listen to poison pill message
    context.system.eventStream.subscribe(self, classOf[PoisonPill])
}