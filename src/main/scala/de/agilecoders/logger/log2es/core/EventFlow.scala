package de.agilecoders.logger.log2es.core

import java.util
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.stream.{FlowMaterializer, MaterializerSettings}

import scala.concurrent.duration.FiniteDuration

/**
 * queue for incoming events
 *
 * @param conf log2es configuration
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class EventFlow[T](conf: Configuration, queue: util.Queue[T], mapper: ILogEventMapper[T], send: Seq[String] => Unit)(implicit actorSystem: ActorSystem) {

  private val settings = MaterializerSettings()
  private val materializer = FlowMaterializer(settings)

  private val fromQueue = Flow(() => queue.poll()).filter(_ != null).toProducer(materializer)
  private val mapped = Flow(fromQueue).map(mapper.map).toProducer(materializer)

  Flow(mapped)
    .groupedWithin(conf.outgoingBulkSize, FiniteDuration(conf.flushQueueTime.toSeconds, TimeUnit.SECONDS))
    .foreach(send)
    .onComplete(materializer)(t => {
    println("stopped")
  })

  def shutdown(): Unit = {}
}
