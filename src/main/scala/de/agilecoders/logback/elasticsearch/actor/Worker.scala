package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import akka.pattern.ask
import akka.routing.{DefaultResizer, RoundRobinRouter, Broadcast}
import ch.qos.logback.classic.spi.ILoggingEvent
import de.agilecoders.logback.elasticsearch._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.util.{Failure, Success, Random}

object Worker {

    /**
     * create actor `Props` for `Worker` actor
     *
     * @return new `Props` instance
     */
    def props() = Props(classOf[Worker])
      .withRouter(RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 2, upperBound = 10))))

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
class Worker() extends Actor with DefaultSupervisor with ActorLogging {
    private[this] val configuration = LogbackContext.configuration
    private[this] var scheduler: Cancellable = Worker.newScheduler(context, configuration)
    private[this] var converter: ActorRef = null
    private[this] var indexSender: ActorRef = null
    private[this] implicit val timeout = configuration.converterTimeout

    /**
         * handle incoming messages.
         */
    def receive = {
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

        case Broadcast(p: PoisonPill) => {
            log.debug("received poison pill via broadcast")

            flush()
            forward(p)
        }

        case p: PoisonPill => {
            log.debug("received poison pill")

            flush()
            forward(p)
        }

        case event: FlushQueue => {
            log.debug("received flush queue action")

            flush()
        }
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
        indexSender ! flushQueue
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
