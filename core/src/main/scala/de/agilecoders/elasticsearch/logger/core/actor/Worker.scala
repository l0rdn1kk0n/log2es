package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor._
import akka.routing.{RoundRobinRouter, Broadcast}
import com.twitter.ostrich.stats.Stats
import de.agilecoders.elasticsearch.logger.core.Log2esContext
import de.agilecoders.elasticsearch.logger.core.actor.Reaper.WatchMe
import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import de.agilecoders.elasticsearch.logger.core.messages.Action._
import de.agilecoders.elasticsearch.logger.core.messages.{Initialize, FlushQueue, Converted}

object Worker {

    /**
     * create actor `Props` for `Worker` actor
     *
     * @return new `Props` instance
     */
    def props(noOfWorkers: Int) = {
        Props(classOf[Worker])
        .withRouter(RoundRobinRouter(nrOfInstances = noOfWorkers))
        .withMailbox("unbounded")
        .withDispatcher("log2es-dispatcher")
    }
}

/**
 * Actor that is responsible for queuing, converting and sending of log messages.
 *
 * @author miha
 */
class Worker() extends Actor with DefaultSupervisor with ActorLogging with DefaultMessageHandler {
    private[this] lazy val configuration = newConfiguration()
    private[this] lazy val converter: ActorRef = newConverter()
    private[this] lazy val indexSender: ActorRef = newSender()

    override protected def onMessage = Stats.time("log2es.worker.onMessageTime") {
        case Converted(message: AnyRef) => indexSender ! message

        case event: AnyRef => converter.tell(event, indexSender)
    }

    override protected def onFlushQueue(message: FlushQueue) = {
        log.debug(s"received flush queue action from $sender")

        flush()
    }

    override protected def onInitialized(log2es: Log2esContext) {
        converter ! Broadcast(Initialize(log2es))
        indexSender ! Broadcast(Initialize(log2es))
    }

    /**
     * forwards the poison pill
     */
    private[this] def forward(pill: PoisonPill): Unit = {
        converter ! Broadcast(pill)
        indexSender ! Broadcast(pill)
    }

    /**
         * flush sender to queue to elasticsearch
         */
    private[this] def flush() {
        indexSender ! Broadcast(flushQueue)
    }

    /**
     * initialize all actors and scheduler before actor is started and
     * receives its first message
     */
    override def preStart() {
        super.preStart()

        log.info(s"startup worker actor: ${hashCode()}")

        context.system.eventStream.publish(WatchMe(self))
    }

    /**
     * after actor was stopped, the scheduler must be stopped too
     */
    override def postStop() {
        flush()
        forward(PoisonPill.getInstance)

        context.stop(converter)
        context.stop(indexSender)

        log.info(s"shutting down worker actor: ${hashCode()}")

        super.postStop()
    }

    /**
     * loads the configuration instance
     */
    protected def newConfiguration(): Configuration = log2es.dependencies.configuration

    /**
     * creates a new converter actor reference
     */
    protected def newConverter(): ActorRef = context.actorOf(Converter.props(), Names.Converter)

    /**
     * creates a new sender actor reference
     */
    protected def newSender(): ActorRef = context.actorOf(IndexSender.props(), Names.Sender)

}
