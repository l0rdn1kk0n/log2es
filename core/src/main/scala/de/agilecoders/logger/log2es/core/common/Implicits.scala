package de.agilecoders.logger.log2es.core.common

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Additional operators.
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object Implicits {

  /**
   * transforms a [[Duration]] into a [[FiniteDuration]]
   *
   * @param d the [[Duration]] to transform
   * @return [[FiniteDuration]] that contains the same time as given [[Duration]]
   */
  implicit def durationToFiniteDuration(d: Duration): FiniteDuration = FiniteDuration(d.toSeconds, TimeUnit.SECONDS)
}
