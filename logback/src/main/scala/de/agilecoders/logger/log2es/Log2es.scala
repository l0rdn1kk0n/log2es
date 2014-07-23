package de.agilecoders.logger.log2es

import java.util.concurrent.atomic.AtomicLong

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.{ILoggingEvent, LoggingEvent, ThrowableProxy}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.Random

/**
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object Log2es extends App {

  System.setProperty("logback.configurationFile", "log2es/logback.xml")
  val logger = LoggerFactory.getLogger("test")

  execute()
  System.exit(0)

  def execute() {
    val r = new Random()
    val time = System.currentTimeMillis()

    val c = new AtomicLong(0)

    try {
      while (System.currentTimeMillis() - time < 2.minutes.toMillis) {
        (100 to (101 + r.nextInt(1000))).toStream.par.foreach(i => {
          log(i)
          c.incrementAndGet()
        })
      }
    } catch {
      case t: Throwable => Console.err.println(t)
    } finally {
      val duration = System.currentTimeMillis() - time
      Console.println(s"count: ${c.get()}; time: ${duration}ms; msg/sec: ${
        if (c.get() > 0) {
          c.get() / (duration / 1000)
        } else {
          "0"
        }
      }")
    }
  }

  def log(i: Int) = {
    val number = scala.math.random * 1000

    number match {
      case n: Double if n < 200 => logger.error("message-" + i, new RuntimeException(new IllegalArgumentException("message")))
      case n: Double if n >= 200 && n < 500 => logger.warn("message-{}", i)
      case n: Double if n >= 500 && n < 900 => logger.info("message-" + i)
      case n: Double if n >= 900 => logger.debug("message-" + i)
    }
  }
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