package de.agilecoders.logback.elasticsearch.actor

import akka.actor._
import ch.qos.logback.classic.spi.ILoggingEvent
import de.agilecoders.logback.elasticsearch.{LogbackContext, IElasticSearchLogbackAppender, CantSendEvent}
import de.agilecoders.logback.elasticsearch.CantSendEvent
import akka.actor.DeadLetter


object ErrorHandler {

    def props(appender: IElasticSearchLogbackAppender): Props = Props(classOf[ErrorHandler], appender)
}

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
class ErrorHandler(appender: IElasticSearchLogbackAppender) extends Actor with ActorLogging {
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
        case e: ILoggingEvent => if(retryOrDiscard(e)) cantSend -= 1
        case _ => appender.addError(failure.message.toString)
    }

    private[this] def handle(deadLetter: DeadLetter) = deadLetter.message match {
        case e: ILoggingEvent => if(retryOrDiscard(e)) deadLetters -= 1
        case _ => appender.addError(deadLetter.message.toString)
    }

    private[this] def retryOrDiscard(message: ILoggingEvent):Boolean = {
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

        case p: PoisonPill => log.debug("received poison pill")
        case x => errors += 1; log.debug(s"got an error for: $x")
    }
}

object RetryLoggingEvent {
    def retry(e: ILoggingEvent): ILoggingEvent = e match {
        case r: RetryLoggingEvent => r.incrementCounter()
        case _ => RetryLoggingEvent(e)
    }
}

case class RetryLoggingEvent(e: ILoggingEvent) extends ILoggingEvent {
    private[this] var counter: Int = 1
    private[this] val lock = new Object

    def incrementCounter(): RetryLoggingEvent = {
        lock.synchronized {
                              counter = counter + 1
                              this
                          }
    }

    def limitReached: Boolean = counter >= 3

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