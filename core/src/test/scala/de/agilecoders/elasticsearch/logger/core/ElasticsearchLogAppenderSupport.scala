package de.agilecoders.elasticsearch.logger.core

import akka.actor.ActorDSL._
import akka.actor.{ActorDSL, ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.twitter.ostrich.stats.{Distribution, Stats}
import de.agilecoders.elasticsearch.logger.core.actor.Reaper.AllSoulsReaped
import de.agilecoders.elasticsearch.logger.core.messages.Action._
import java.util.concurrent.TimeUnit
import org.elasticsearch.common.base.Stopwatch
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._

/**
 * helper class for all appender tests
 *
 * @author miha
 */
abstract class ElasticsearchLogAppenderSupport(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with
WordSpecLike with Matchers with BeforeAndAfterAll {
    var timeout: FiniteDuration = 15.seconds
    val timer: Stopwatch = new Stopwatch()

    override protected final def afterAll(): Unit = {
        try {
            stop()
            expectMsg(timeout, "Dead")
        } finally {
            timer.stop()

            Console.out.append("\n\n-----------\nMetrics:\n")
            Stats.get().metrics foreach (logMetrics _)
            Console.out.append("\n\nCounters:\n")
            Stats.get().counters foreach (logCounters _)
            Console.out.append("\n\nGauges:\n")
            Stats.get().gauges foreach (logGauges _)
            Console.out.append("\n\nDuration: " + timer.elapsed(TimeUnit.MILLISECONDS) + "ms")
        }
    }

    private def logMetrics(t: (String, Distribution)): Unit = Console.out.append(s"${t._1}: ${t._2.toJson()}\n")

    private def logCounters(t: (String, Long)): Unit = Console.out.append(s"${t._1}: ${t._2}\n")

    private def logGauges(t: (String, Double)): Unit = Console.out.append(s"${t._1}: ${t._2}\n")

    override protected final def beforeAll(): Unit = {
        timer.start()
        start()

        val allSoulsReapedWatcher: ActorRef = ActorDSL.actor(new Act {
            become {
                       case message: AllSoulsReaped => testActor ! "Dead"
                   }
        })
        _system.eventStream.subscribe(allSoulsReapedWatcher, classOf[AllSoulsReaped])
    }

    protected final def waitForEmptyQueue() {
        Thread.sleep(5000) // give messages a chance to flow ;)
        router() ! alive

        expectMsg(timeout, imAlive)
    }

    def start(): Unit

    def stop(): Unit

    def router(): ActorRef
}
