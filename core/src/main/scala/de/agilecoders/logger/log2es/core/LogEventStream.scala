package de.agilecoders.logger.log2es.core

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import de.agilecoders.logger.log2es.core.elasticsearch.{ESClient, Response}
import de.agilecoders.logger.log2es.core.mapper.EventMapper

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * interface for event streams
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait EventStream[T] {
  private val running = new AtomicBoolean(false)

  /**
   * adds an event to the processing queue
   *
   * @param event the event to add
   * @return true if given event was queued
   */
  def enqueue(event: T): Boolean

  /**
   * starts the event stream
   */
  def start(): Unit

  /**
   * shutdown event stream
   */
  def shutdown(): Unit

  /**
   * @return true, if event stream is running
   */
  final def isRunning: Boolean = running.get()

  /**
   * marks this event stream as started
   *
   * @return this instance for chaining
   */
  protected final def started(): this.type = {
    running.set(true)
    this
  }

  /**
   * marks this event stream as stopped
   *
   * @return this instance for chaining
   */
  protected final def stopped(): this.type = {
    running.set(true)
    this
  }
}

/**
 * queue for incoming events
 *
 * @param conf log2es configuration
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class LogEventStream[T](conf: Configuration = Configuration(), mapper: EventMapper[T]) extends EventStream[T] {
  private lazy val activeRequests = ArrayBuffer[Future[Response]]()

  private var flow: EventFlow[T] = _
  private var client: ESClient = _
  private var listener: Option[String => Unit] = None

  private val queue: ProcessingQueue[T] = QueueFactory.create[T](conf)

  override def enqueue(event: T): Boolean = queue.offer(event, conf.defaultTimeout.duration.toSeconds, TimeUnit.SECONDS)

  private def send(events: Seq[String]): Unit = {
    implicit val ec = flow.executionContext
    val response = client.send(conf.indexName, conf.dynamicTypeName, events)

    activeRequests += response

    response.onComplete(r => {
      activeRequests -= response

      r match {
        case Failure(e) =>
          Console.err.println(e)
        case Success(resp) =>
        //Console.println(s"send successfully: ${events.length}")
      }
    })
  }

  override def start() = {
    implicit val ec = scala.concurrent.ExecutionContext.global

    val client = ESClient.create(conf)
    client.start()

    conf.updateMapping match {
      case true =>
        val response = client.updateIndex(conf.indexName, conf.dynamicTypeName, Configuration.loadMapping(conf.esConfigurationFile, conf.dynamicTypeName, conf.ttl))

        response.onComplete({
          case Success(r) => // mapping created
          case Failure(e) => throw e
        })

        // block until mapping was created
        Await.result(response, conf.defaultTimeout.duration)
      case false => // nothing to do, mapping up2date
    }

    this.listener = Some(conf.onTypeNameChanged(newTypeName => {
      val response = client.updateIndex(conf.indexName, newTypeName, Configuration.loadMapping(conf.esConfigurationFile, newTypeName, conf.ttl))
      Await.result(response, conf.defaultTimeout.duration)
    }))
    this.client = client
    this.flow = EventFlow(conf, queue, mapper, send)

    started()
  }

  override def shutdown() = {
    stopped()

    listener.foreach(conf.removeListener)
    flow.shutdown()

    try {
      activeRequests.filter(!_.isCompleted).foreach(Await.result(_, conf.defaultTimeout.duration))
    } catch {
      case t: Throwable => Console.err.println("can't await queued requests")
    }

    client.shutdown()

    listener = None
    activeRequests.clear()
    queue.clear()
  }

}


object QueueFactory {

  def create[T](conf: Configuration): ProcessingQueue[T] = new ArrayBlockingQueue[T](conf.incomingBufferSize) with ProcessingQueue[T]

}

trait ProcessingQueue[T] {
  def offer(e: T, timeout: Long, unit: TimeUnit): Boolean

  def poll(): T

  def clear(): Unit
}