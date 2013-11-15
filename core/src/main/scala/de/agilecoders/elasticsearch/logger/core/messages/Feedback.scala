package de.agilecoders.elasticsearch.logger.core.messages

/**
 * a base feedback message
 */
sealed trait Feedback

/**
 * message that is thrown if a log message can't be sent
 */
case class CantSendEvent(message: AnyRef) extends Feedback

/**
 * an error message is thrown if there is an exception which wasn't handled
 */
case class Error(throwable: Throwable) extends Feedback

/**
 * an error that is thrown when a elasticsearch error occures
 */
case class ElasticsearchError(throwable: Throwable) extends Feedback