package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor._
import de.agilecoders.elasticsearch.logger.core.Log2esAppender
import de.agilecoders.elasticsearch.logger.logger.{ElasticsearchError, CantSendEvent, FlushQueue}
import java.util.concurrent.atomic.AtomicInteger


object ErrorHandler {

    def props(appender: Log2esAppender[_]): Props = Props(classOf[ErrorHandler], appender)
}

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
class ErrorHandler(appender: Log2esAppender[_]) extends Actor with ActorLogging {
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

    private[this] def handle(failure: CantSendEvent) = failure.message match {
        case e: ILoggingEvent => if (retryOrDiscard(e)) {
            cantSend -= 1
        }
        case _ => appender.addError(failure.message.toString)
    }

    private[this] def handle(deadLetter: DeadLetter) = deadLetter.message match {
        case e: ILoggingEvent => if (retryOrDiscard(e)) {
            deadLetters -= 1
        }
        case _ => log.debug(s"ignored deadletter message: ${deadLetter.message} sent from ${deadLetter.sender} to ${deadLetter.recipient}")
    }

    private[this] def retryOrDiscard(message: ILoggingEvent): Boolean = {
        appender.isDiscardable(message) match {
            case true => log.warning(s"dropped discardable event: $message"); false
            case false => {
                val event = RetryLoggingEvent(message)

                event.limitReached match {
                    case true => log.error(s"dropped non-discardable event: $event"); false
                    case false => appender.doAppend(event); true
                }
            }
        }
    }

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

            appender.addError(s"${e.getMessage}", e)
        }

        case message: FlushQueue => log.debug("ignore flush messages because there's no queue to flush")
        case p: PoisonPill => log.debug("received poison pill")

        case u: UnhandledMessage => handleUnhandledMessage(u)
        case x => handleUnhandledMessage(x)
    }

    private def handleUnhandledMessage(message: UnhandledMessage): Unit = {
        if (!ignorable.contains(message.message.getClass)) {
            errors += 1

            log.warning(s"unhandled message: ${message.message} sent from ${message.sender.path} to ${message.recipient.path}")
        }
    }

    private def handleUnhandledMessage(message: Any): Unit = {
        if (!ignorable.contains(message.getClass)) {
            errors += 1

            log.warning(s"unhandled message: $message sent from ${sender.path}")
        }
    }
}

protected object RetryLoggingEvent {
    /**
     * wraps a given ILoggingEvent with a RetryLoggingEvent or increments
     * the RetryLoggingEvent counter when a RetryLoggingEvent is given.
     */
    def retry(e: ILoggingEvent): ILoggingEvent = e match {
        case event: RetryLoggingEvent => {
            event.incrementCounter()
            event
        }

        case _ => RetryLoggingEvent(e)
    }
}

/**
 * A RetryLoggingEvent wraps an existing ILoggingEvent and keeps track of
 * number of retries.
 */
protected case class RetryLoggingEvent(e: ILoggingEvent,
                                       maxRetries: Int = Log2esContext.configuration.retryCount) extends ILoggingEvent {
    private[this] lazy val counter = new AtomicInteger(1)

    def incrementCounter(): Int = counter.incrementAndGet()

    def limitReached: Boolean = counter.get() >= maxRetries

    def prepareForDeferredProcessing() = e.prepareForDeferredProcessing()

    def getThreadName = e.getThreadName

    def getLevel = e.getLevel

    def getMessage = e.getMessage

    def getArgumentArray = e.getArgumentArray

    def getFormattedMessage = e.getFormattedMessage

    def getLoggerName = e.getLoggerName

    def getLoggerContextVO = e.getLoggerContextVO

    def getThrowableProxy = e.getThrowableProxy

    def getCallerData = e.getCallerData

    def hasCallerData = e.hasCallerData

    def getMarker = e.getMarker

    def getMDCPropertyMap = e.getMDCPropertyMap

    @Deprecated
    def getMdc = e.getMdc

    def getTimeStamp = e.getTimeStamp
}