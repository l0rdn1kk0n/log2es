package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import akka.pattern.ask
import akka.routing.{RoundRobinRouter, Broadcast}
import ch.qos.logback.classic.spi.ILoggingEvent
import de.agilecoders.logback.elasticsearch._
import de.agilecoders.logback.elasticsearch.conf.Configuration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.util.{Failure, Success, Random}
import de.agilecoders.logback.elasticsearch.actor.Reaper.WatchMe

object Worker {

    /**
     * create actor `Props` for `Worker` actor
     *
     * @return new `Props` instance
     */
    def props() = Props(classOf[Worker]).withRouter(RoundRobinRouter(nrOfInstances = 10))

    /**
     * create and start a new scheduler that sends `FlushQueue` messages to given `Worker` actor.
     *
     * @param context the actor context
     * @param configuration the configuration
     * @return new scheduler
     */
    protected[Worker] def newScheduler(context: ActorContext, configuration: Configuration): Cancellable = {
        val startDelay = Duration(100 + new Random().nextInt(configuration.flushInterval), TimeUnit.MILLISECONDS)
        val interval = Duration(configuration.flushInterval, TimeUnit.MILLISECONDS)

        context.system.scheduler.schedule(startDelay, interval, context.self, flushQueue)
    }

}

/**
 * Actor that is responsible for queuing, converting and sending of log messages.
 *
 * @author miha
 */
class Worker() extends Actor with DefaultSupervisor with ActorLogging with DefaultMessageHandler {
    private[this] val configuration = Log2esContext.configuration
    private[this] var scheduler: Cancellable = Worker.newScheduler(context, configuration)
    private[this] var converter: ActorRef = null
    private[this] var indexSender: ActorRef = null
    private[this] implicit val timeout = configuration.converterTimeout

    override protected def onMessage = {
        case event: ILoggingEvent => {
            log.debug(s"received log event: ${event.hashCode()}")

            val _sender = sender
            val future = converter ? event

            future onComplete {
                case Success(result) => {
                    log.debug(s"redirect event [${event.hashCode()}] to index sender")

                    indexSender ! result.asInstanceOf[AnyRef]
                }

                case Failure(failure) => {
                    log.debug(s"received conversion failure for event [${event.hashCode()}]; respond to sender.")

                    _sender ! CantSendEvent(event)
                }
            }

        }
    }

    override protected def onFlushQueue(message: FlushQueue) = {
        log.debug(s"received flush queue action from $sender")

        flush()
    }

    override protected def onPoisonPill(message: PoisonPill) = {
        log.debug(s"received poison pill from $sender, flush and forward")

        flush()
        forward(message)
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

        scheduler = scheduler.isCancelled match {
            case true => Worker.newScheduler(context, configuration)
            case _ => scheduler
        }

        converter = Creator.newConverter(context)
        indexSender = Creator.newIndexSender(context)

        context.system.eventStream.publish(WatchMe(self))
    }

    /**
     * after actor was stopped, the scheduler must be stopped too
     */
    override def postStop() {
        scheduler.cancel()

        context.stop(converter)
        context.stop(indexSender)

        log.info(s"shutting down worker actor: ${hashCode()}")

        super.postStop()
    }

}
