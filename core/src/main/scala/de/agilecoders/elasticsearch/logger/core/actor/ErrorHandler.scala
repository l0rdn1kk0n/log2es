package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor._
import de.agilecoders.elasticsearch.logger.core.messages.{ElasticsearchError, CantSendEvent, FlushQueue}


object ErrorHandler {

    def props(): Props = Props(classOf[ErrorHandler])
}

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
class ErrorHandler() extends Actor with ActorLogging {
    private[this] val ignorable = Seq(classOf[FlushQueue])
    private[this] var errors = 0
    private[this] var deadLetters = 0
    private[this] var cantSend = 0

    override def preStart() = {
        super.preStart()

        log.info("starting ErrorHandler")
    }

    override def postStop() = {
        log.info(s"shutting down ErrorHandler; errors: $errors; cantSend: $cantSend; deadLetters: $deadLetters")

        super.postStop()
    }

    protected def handle(failure: CantSendEvent) = failure.message match {
        case e: AnyRef => if (retryOrDiscard(e)) {
            cantSend -= 1
        }
        case _ => cantHandle(failure.message)
    }

    protected def handle(deadLetter: DeadLetter) = deadLetter.message match {
        case e: AnyRef => if (retryOrDiscard(e)) {
            deadLetters -= 1
        }
        case _ => log.debug(s"ignored deadletter message: ${deadLetter.message} sent from ${deadLetter.sender} to ${deadLetter.recipient}")
    }

    protected def cantHandle(message: AnyRef): Unit = {}

    protected def retryOrDiscard(message: AnyRef): Boolean = false

    def receive = {
        case f: CantSendEvent => {
            log.debug(s"got an cant send event: ${f.hashCode()}")

            cantSend += 1
            handle(f)
        }
        case d: DeadLetter => {
            log.debug(s"got an dead letter: ${d.hashCode()}")

            deadLetters += 1
            handle(d)
        }

        case ElasticsearchError(e: Throwable) => {
            log.error(s"${e.getMessage}", e)
        }

        case message: FlushQueue => log.debug("ignore flush messages because there's no queue to flush")
        case p: PoisonPill => log.debug("received poison pill")

        case u: UnhandledMessage => handleUnhandledMessage(u)
        case x => handleUnhandledMessage(x)
    }

    protected def handleUnhandledMessage(message: UnhandledMessage): Unit = {
        if (!ignorable.contains(message.message.getClass)) {
            errors += 1

            log.warning(s"unhandled message: ${message.message} sent from ${message.sender.path} to ${message.recipient.path}")
        }
    }

    protected def handleUnhandledMessage(message: Any): Unit = {
        if (!ignorable.contains(message.getClass)) {
            errors += 1

            log.warning(s"unhandled message: $message sent from ${sender.path}")
        }
    }
}
