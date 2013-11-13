package de.agilecoders.elasticsearch.logger.core.mapper

import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.helpers.BasicMarkerFactory
import scala.unchecked
import scala.util.parsing.json.JSON
import de.agilecoders.elasticsearch.logger.logback.mapper.LoggingEventToXContentMapper

/**
 * Tests the `LoggingEventToXContentMapperSpec`
 *
 * @author miha
 */
class LoggingEventToXContentMapperSpec extends WordSpecLike with Matchers {
    val mapper = LoggingEventToXContentMapper(CustomizableConfiguration())

    "A mapper" must {

        "add the logging timestamp" in {
            val event = new LoggingEvent()
            event.setTimeStamp(12345)

            mapAndCheck(event, "timestamp", 12345)
        }

        "add the logging date" in {
            val event = new LoggingEvent()
            event.setTimeStamp(12345)

            mapAndCheck(event, "date", "1970-01-01T01:00:12.345+01:00")
        }

        "add the log level" in {
            val event = new LoggingEvent()
            event.setLevel(Level.WARN)

            mapAndCheck(event, "level", "WARN")
        }

        "add the logger name" in {
            val event = new LoggingEvent()
            event.setLoggerName("logger-name")

            mapAndCheck(event, "logger", "logger-name")
        }

        "add the marker" in {
            val event = new LoggingEvent()
            event.setMarker(new BasicMarkerFactory().getMarker("marker-name"))

            mapAndCheck(event, "marker", "marker-name")
        }

        "add the arguments" in {
            val event = new LoggingEvent()
            val arr = Array[Object]("string", new Integer(12345), Boolean.box(true))

            event.setArgumentArray(arr)

            mapAndCheck(event, "arguments", Seq("string", "12345", "true"))
        }

        "add the thread that has logged this event" in {
            val event = new LoggingEvent()
            event.setThreadName("thread-name")

            mapAndCheck(event, "thread", "thread-name")
        }
    }

    @unchecked
    private def mapAndCheck(event: ILoggingEvent, key: String, expected: Any) = {
        val json = mapper.map(event).string()

        JSON.parseFull(json) match {
            case Some(m: Map[String, Any]) => {
                m.get(key).getOrElse(fail()) match {
                    case seq: Seq[AnyRef] => {
                        val expectedSeq = expected.asInstanceOf[Seq[AnyRef]]
                        seq should have length expectedSeq.size

                        seq foreach (expectedSeq should contain(_))
                    }
                    case other => other should be(expected)
                }
            }
            case x => fail(x.getClass.getName)
        }
    }

}
