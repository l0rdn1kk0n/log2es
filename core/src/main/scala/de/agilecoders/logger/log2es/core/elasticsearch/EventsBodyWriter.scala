package de.agilecoders.logger.log2es.core.elasticsearch

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

import com.ning.http.client.Request.EntityWriter

/**
 * A special body writer that can write data plain or gzipped.
 *
 * @param events the events to write to output stream
 * @param useGzip whether to compress data or not
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class EventsBodyWriter(events: Seq[String], useGzip: Boolean = false) extends EntityWriter {
  private val action = """{ "index" : { } }""" + "\n"

  override def writeEntity(out: OutputStream): Unit = useGzip match {
    case true =>
      writeTo(new GZIPOutputStream(out)).finish()
    case false =>
      writeTo(out)
  }

  private def writeTo[T <: OutputStream](out: T): T = {
    events.map(action + _ + "\n").foreach(v => out.write(v.getBytes(StandardCharsets.UTF_8)))
    out
  }
}
