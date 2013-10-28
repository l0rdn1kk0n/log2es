package de.agilecoders.logback.elasticsearch.actor

import akka.actor.SupervisorStrategy.{Restart, Escalate, Resume}
import akka.actor._
import akka.routing.{DefaultResizer, RoundRobinRouter}
import de.agilecoders.logback.elasticsearch.IElasticSearchLogbackAppender
import scala.concurrent.duration._

/**
 * The `Creator` creates new `ActorRef` instances for all library actors.
 *
 * @author miha
 */
object Creator {

    private[this] lazy val system = LogbackActorSystem.instance

    def newRouter(appender: IElasticSearchLogbackAppender): ActorRef = {
        val actor = system.actorOf(Router.props(appender), "router")
        system.eventStream.subscribe(actor, classOf[PoisonPill])

        actor
    }

    def newWorker(context: ActorContext): ActorRef = {
        val actor = context.actorOf(enhance(Worker.props()), "queued-worker")
        system.eventStream.subscribe(actor, classOf[PoisonPill])

        actor
    }

    def newIndexSender(context: ActorContext): ActorRef = {
        val actor = context.actorOf(IndexSender.props(), "sender")
        system.eventStream.subscribe(actor, classOf[PoisonPill])


        actor
    }

    def newErrorHandler(context: ActorContext, appender: IElasticSearchLogbackAppender): ActorRef = {
        val actor = context.actorOf(ErrorHandler.props(appender), "error-handler")
        system.eventStream.subscribe(actor, classOf[DeadLetter])
        system.eventStream.subscribe(actor, classOf[PoisonPill])

        actor
    }

    def newConverter(context: ActorContext): ActorRef = {
        context.actorOf(Converter.props(), "converter")
    }

    def newRestartingSupervisor() = OneForOneStrategy(maxNrOfRetries = 10,
                                            withinTimeRange = 1 minute) {
                                                                            case _: RuntimeException => Restart
                                                                            case _: Exception => Escalate
                                                                        }

    def newSupervisor() = OneForOneStrategy(maxNrOfRetries = 10,
                                            withinTimeRange = 1 minute) {
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