package de.agilecoders.logger.log2es.core

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import akka.actor.{ActorSystem, PoisonPill}
import de.agilecoders.logger.log2es.core.elasticsearch.{ESClient, Response}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * queue for incoming events
 *
 * @param conf log2es configuration
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class LogEventStream[T](conf: Configuration = Configuration(), mapper: ILogEventMapper[T]) {
  implicit val timeout = conf.defaultTimeout
  implicit val sys = ActorSystem.create(conf.actorSystemName)
  implicit val ec = sys.dispatcher

  private lazy val activeRequests = ArrayBuffer[Future[Response]]()

  private lazy val client = {
    val client = ESClient.create(conf)
    client.start()

    client
  }

  private val queue = new ArrayBlockingQueue[T](conf.incomingBufferSize)
  private val flow = EventFlow(conf, queue, mapper, send)

  def enqueue(event: T): Boolean = queue.offer(event, timeout.duration.toSeconds, TimeUnit.SECONDS)

  private def send(events: Seq[String]): Unit = {
    val response = client.send(conf.indexName, conf.typeName, events)

    activeRequests += response

    response.onComplete(r => {
      activeRequests -= response

      r match {
        case Failure(e) => Console.err.println(e)
        case Success(resp) =>
      }
    })
  }

  def shutdown() = {
    flow.shutdown()
    sys.actorSelection("/user/*").tell(PoisonPill.getInstance, null)
    sys.shutdown()

    activeRequests.filter(!_.isCompleted).foreach(Await.result(_, timeout.duration))
    client.shutdown()
  }

}


