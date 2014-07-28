package de.agilecoders.logger.log2es.core

import akka.actor.{ActorSystem, PoisonPill}
import akka.stream.scaladsl.Flow
import akka.stream.{FlowMaterializer, MaterializerSettings}
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import de.agilecoders.logger.log2es.core.mapper.EventMapper

import scala.concurrent.duration._
import scala.util.Success

/**
 * queue for incoming events
 *
 * @param conf log2es configuration
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class EventFlow[T](conf: Configuration, queue: ProcessingQueue[T], mapper: EventMapper[T], send: Seq[String] => Unit) {

  import de.agilecoders.logger.log2es.core.common.Implicits.durationToFiniteDuration

  implicit val timeout = conf.defaultTimeout
  implicit val sys = ActorSystem.create(conf.actorSystemName, akkaConf)
  implicit val executionContext = sys.dispatcher

  private lazy val akkaConf: Config = {
    val base = ConfigFactory.load()
    var custom: Option[Config] = None

    try {
      custom = Some(base.getConfig(conf.actorSystemName))
    } catch {
      case e: ConfigException.Missing =>
        custom = None
    }

    custom match {
      case Some(c) => c.withFallback(base)
      case None => base
    }
  }

  private val settings = MaterializerSettings()
  private val materializer = FlowMaterializer(settings)

  private val fromQueue = Flow(() => queue.poll()).filter(_ != null).toProducer(materializer)
  private val mapped = Flow(fromQueue).map(mapper.mapToString).toProducer(materializer)

  Flow(mapped)
    .groupedWithin(conf.outgoingBulkSize, conf.flushQueueTime)
    .foreach(send)
    .onComplete(materializer)({
    case Success(r) => // stopped successfully
    case _ => Console.err.append("can't stop event flow")
  })

  val scheduler = conf.typeNameUpdateInterval match {
    case Duration.Zero => None
    case Duration.Undefined => None
    case Duration.MinusInf => None
    case duration: Duration if duration.toSeconds > 0 =>
      Some(sys.scheduler.schedule(duration, duration) {
        conf.updateTypeName()
      })
    case _ => None
  }

  /**
   * stop akka-stream
   */
  def shutdown(): Unit = {
    scheduler.filter(!_.isCancelled).foreach(_.cancel())

    // TODO miha: check how to shutdown akka-stream, shutdown actor system is enough?
    sys.actorSelection("/user/*").tell(PoisonPill.getInstance, null)
    sys.shutdown()
    sys.awaitTermination(conf.defaultTimeout.duration)
  }

}
