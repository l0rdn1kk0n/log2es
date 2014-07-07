package de.agilecoders.logger.log2es

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.{ILoggingEvent, LoggingEvent, ThrowableProxy}
import de.agilecoders.logger.log2es.logback.ElasticsearchAppender

/**
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object Log2es extends App {

  val stream = ElasticsearchAppender()

  def run() {
    var x = 0
    while (x < 1009) {
      x = x + 1
      stream.append(Factory.newEvent(x))

      x % 10000 match {
        case 0 => Thread.sleep((Math.random() * 6000).asInstanceOf[Long])
        case _ =>
      }
    }
  }

  stream.setClientType("http")
  stream.setClusterName("elasticsearch_miha")
  stream.start()
  run()
  println("finished round 1")
  Thread.sleep(6000)
  run()
  println("finished round 2")
  Thread.sleep(6000)
  stream.stop()
}

object Factory {

  import scala.collection.JavaConversions._

  def newEvent(): ILoggingEvent = newEvent(scala.math.random)

  def newEvent(number: Double): ILoggingEvent = {
    val event = new LoggingEvent()

    event.setMessage("message-" + number)
    event.setThreadName("thread-" + number)
    event.setLevel(Level.WARN)
    event.setLoggerName("logger-" + number)
    event.setTimeStamp(System.currentTimeMillis())

    if (number > 0.7) {
      event.setThrowableProxy(new ThrowableProxy(new RuntimeException("exception message " + number)))
    }

    if (number > 0.5) {
      event.setArgumentArray(Array(Double.box(number), Boolean.box(number > 0.75), "hello-" + number))
    }

    if (number < 0.2) {
      //event.setMarker(new BasicMarker("name-" + number))
      //event.setCallerData(A)
    }

    if (number > 0.8) {
      event.setMDCPropertyMap(Map[String, String](
        "sessionId" -> "sid",
        "requestId" -> "1234"
      ))
    }

    event
  }
}