package de.agilecoders.logger.log2es.core

/**
 * Created by miha on 07.07.14.
 */
trait ILogEventMapper[T] {

  def map(event: T): String

}
