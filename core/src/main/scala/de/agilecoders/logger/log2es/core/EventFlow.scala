package de.agilecoders.logger.log2es.core

import java.util
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, PoisonPill}
import akka.stream.scaladsl.Flow
import akka.stream.{FlowMaterializer, MaterializerSettings}
import com.typesafe.config.{Config, ConfigFactory}
import de.agilecoders.logger.log2es.core.mapper.EventMapper

import scala.concurrent.duration._

/**
 * queue for incoming events
 *
 * @param conf log2es configuration
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class EventFlow[T](conf: Configuration, queue: util.Queue[T], mapper: EventMapper[T], send: Seq[String] => Unit) {

  implicit val timeout = conf.defaultTimeout
  implicit val sys = ActorSystem.create(conf.actorSystemName, akkaConf)
  implicit val executionContext = sys.dispatcher

  private lazy val akkaConf: Config = {
    val base = ConfigFactory.load()

    base.getConfig(conf.actorSystemName).withFallback(base)
  }

  private val settings = MaterializerSettings()
  private val materializer = FlowMaterializer(settings)

  private val fromQueue = Flow(() => queue.poll()).filter(_ != null).toProducer(materializer)
  private val mapped = Flow(fromQueue).map(mapper.mapToString).toProducer(materializer)

  Flow(mapped)
    .groupedWithin(conf.outgoingBulkSize, FiniteDuration(conf.flushQueueTime.toSeconds, TimeUnit.SECONDS))
    .foreach(send)
    .onComplete(materializer)(t => {
    println("stopped")
  })

  sys.scheduler.schedule(30.seconds, 30.seconds) {
    conf.updateTypeName()
  }

  /**
   * stop akka-stream
   */
  def shutdown(): Unit = {
    // TODO miha: check how to shutdown akka-stream, shutdown actor system is enough?
    sys.actorSelection("/user/*").tell(PoisonPill.getInstance, null)
    sys.shutdown()
    sys.awaitTermination(conf.defaultTimeout.duration)
  }
}
