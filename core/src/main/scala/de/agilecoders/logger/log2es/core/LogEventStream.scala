package de.agilecoders.logger.log2es.core

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import de.agilecoders.logger.log2es.core.elasticsearch.{ESClient, Response}
import de.agilecoders.logger.log2es.core.mapper.EventMapper

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * queue for incoming events
 *
 * @param conf log2es configuration
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class LogEventStream[T](conf: Configuration = Configuration(), mapper: EventMapper[T]) {
  private lazy val activeRequests = ArrayBuffer[Future[Response]]()

  private lazy val client = {
    implicit val ec = scala.concurrent.ExecutionContext.global

    val client = ESClient.create(conf)
    client.start()

    conf.updateMapping match {
      case true =>
        val response = client.updateIndex(conf.indexName, conf.dynamicTypeName, Configuration.loadMapping(conf.esConfigurationFile, conf.typeName, conf.ttl))

        response.onComplete({
          case Success(r) => // mapping created
          case Failure(e) => throw e
        })

        // block until mapping was created
        Await.result(response, conf.defaultTimeout.duration)
      case false => // nothing to do, mapping up2date
    }

    client
  }

  private val queue = new ArrayBlockingQueue[T](conf.incomingBufferSize)
  private val flow = EventFlow(conf, queue, mapper, send)
  implicit val executionContext = flow.executionContext

  conf.onTypeNameChanged( newTypeName => {
    println(s"onTypeNameChanged($newTypeName)")

    val response = client.updateIndex(conf.indexName, newTypeName, Configuration.loadMapping(conf.esConfigurationFile, newTypeName, conf.ttl))
    Await.result(response, conf.defaultTimeout.duration)
  })

  def enqueue(event: T): Boolean = queue.offer(event, conf.defaultTimeout.duration.toSeconds, TimeUnit.SECONDS)

  private def send(events: Seq[String]): Unit = {
    val response = client.send(conf.indexName, conf.dynamicTypeName, events)

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

    activeRequests.filter(!_.isCompleted).foreach(Await.result(_, conf.defaultTimeout.duration))
    client.shutdown()
  }

}


