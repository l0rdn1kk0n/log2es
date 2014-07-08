package de.agilecoders.logger.log2es.core.elasticsearch

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

import com.ning.http.client.Request.EntityWriter
import de.agilecoders.logger.log2es.core.Configuration

import scala.concurrent.{Future, promise}
import scala.util.{Failure, Success}

/**
 * elasticsearch client factory
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
object ESClient {

  /**
   * creates an elasticsearch client according to given client configuration
   *
   * @param conf the client configuration
   * @return new elasticsearch client
   */
  def create(conf: Configuration) = conf.clientType match {
    case "noop" => new ESNoopClient(conf)

    case "http" => new ESHttpClient(conf)

    case "native" => new ESNoopClient(conf)

    case clientType => throw new IllegalArgumentException(s"there's no such elasticsearch client: $clientType")
  }

}


/**
 * elasticsearch client interface
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait ESClient {

  def send(indexName: String, typeName: String, events: Seq[String]): Future[Response]

  def updateIndex(indexName: String, typeName: String, configuration: String): Future[Response]

  def status(): Future[String]

  def shutdown(): Unit

  def start(): Unit
}

/**
 * response abstraction
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait Response {

  def statusCode: Int

  def body: String

}


case class ESHttpClient(conf: Configuration) extends ESClient {

  import dispatch.Defaults._
  import dispatch._

  private lazy val hostname = host("localhost", 9200)

  private lazy val indexRequestBuilder = (hostname / conf.indexName).POST

  def createIndex(indexName: String, content: String): scala.concurrent.Future[Response] = {
    val r = (hostname / indexName).PUT.setBody(content)

    execute(r)
  }

  override def send(indexName: String, typeName: String, events: Seq[String]) = {
    val content = EventsBodyWriter(events, conf.gzip)
    val request = indexRequestBuilder / typeName / "_bulk"

    execute(request setBody content)
  }

  override def updateIndex(indexName: String, typeName: String, configuration: String) = {
    execute((hostname / indexName).POST.setBody(configuration))
  }

  override def status() = {
    execute(hostname / "_cluster" / "health" GET) map (_.body)
  }

  private def execute(req: Req): scala.concurrent.Future[Response] = {
    val f = promise[Response]()

    Http(req
      .setContentType("application/json", "UTF-8")
      .setHeader("User-Agent", conf.userAgent)).onComplete({
      case Success(response) =>
        f.complete(Success(HttpResponse(response)))
      case Failure(e) =>
        f.failure(e)
    })

    f.future
  }

  override def shutdown() = {
    Http.shutdown()
  }

  override def start() = {}
}

case class ESNoopClient(conf: Configuration) extends ESClient {

  override def shutdown(): Unit = {
    println("ESClient.shutdown")
  }

  override def status() = Future.successful("{\ncluster_name: \"elasticsearch_miha\",\nstatus: \"green\",\ntimed_out: false,\nnumber_of_nodes: 1,\nnumber_of_data_nodes: 1,\nactive_primary_shards: 0,\nactive_shards: 0,\nrelocating_shards: 0,\ninitializing_shards: 0,\nunassigned_shards: 0\n}")

  override def send(indexName: String, typeName: String, events: Seq[String]): Future[Response] = {
    println(s"ESClient.send($indexName, $typeName, ${events.length})")
    Future.successful(StaticResponse(200))
  }

  override def updateIndex(indexName: String, typeName: String, mapping: String) = {
    println(s"ESClient.putMapping($indexName, $typeName, $mapping)")
    Future.successful(StaticResponse(200))
  }

  override def start(): Unit = {
    println("ESClient.start")
  }

}

case class EventsBodyWriter(events: Seq[String], useGzip: Boolean = false) extends EntityWriter {
  private val action = """{ "index" : { } }""" + "\n"

  override def writeEntity(out: OutputStream): Unit = useGzip match {
    case true =>
      val gzip = new GZIPOutputStream(out)
      events.map(action + _ + "\n").foreach(v => gzip.write(v.getBytes(StandardCharsets.UTF_8)))
      gzip.finish()
    case false =>
      events.map(action + _ + "\n").foreach(v => out.write(v.getBytes(StandardCharsets.UTF_8)))
  }
}

case class StaticResponse(statusCode: Int, body: String = "") extends Response

case class HttpResponse(private val response: com.ning.http.client.Response) extends Response {
  override def statusCode: Int = response.getStatusCode

  override def body: String = response.getResponseBody
}