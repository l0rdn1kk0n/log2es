package de.agilecoders.logger.log2es.log4j2

import java.util.concurrent.atomic.AtomicLong

import org.apache.logging.log4j.LogManager

import scala.concurrent.duration._
import scala.util.Random

/**
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object Log2es extends App {
  System.setProperty("log4j.configurationFile", "log2es/log4j.xml")
  val logger = LogManager.getLogger("test")

  execute()
  System.exit(0)

  def execute() {
    val r = new Random()
    val time = System.currentTimeMillis()

    val c = new AtomicLong(0)

    try {
      while (System.currentTimeMillis() - time < 10.seconds.toMillis) {
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
      case n: Double if n >= 200 && n < 500 => logger.warn("message-{}", i.toString)
      case n: Double if n >= 500 && n < 900 => logger.info("message-" + i)
      case n: Double if n >= 900 => logger.debug("message-" + i)
    }
  }
}
