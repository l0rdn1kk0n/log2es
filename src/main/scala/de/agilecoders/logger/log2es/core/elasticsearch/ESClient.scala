package de.agilecoders.logger.log2es.core.elasticsearch

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

  private lazy val request = (url("http://localhost:9200") / conf.indexName)
    .POST
    //.setContentType("application/json", StandardCharsets.UTF_8.name())
    .setHeader("User-Agent", conf.userAgent)

  private def mapEvent(event: String): String = {
    s"""{ "index" : { "_ttl" : "90d" } }""" + "\n" + event
  }

  private def toBody(events: Seq[String]): String = events.map(mapEvent).mkString("\n")

  override def send(indexName: String, typeName: String, events: Seq[String]) = {
    val f = promise[Response]()
    val r = (request / conf.dynamicTypeName / "_bulk") setBody toBody(events)

    Http(r).onComplete({
      case Success(response) =>
        println("BODY: " + response.getResponseBody)
        f.complete(Success(HttpResponse(response)))
      case Failure(e) =>
        f.failure(e)
    })

    f.future
  }

  override def status() = {
    Http(host("localhost", 9200) / "_cluster" / "health" GET) map { value =>
      value.getResponseBody
    }
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
    println(s"ESClient.send($indexName, $typeName, ${events.length}})")
    Future.successful(StaticResponse(200))
  }

  override def start(): Unit = {
    println("ESClient.start")
  }

}

case class StaticResponse(statusCode: Int, body: String = "") extends Response

case class HttpResponse(private val response: com.ning.http.client.Response) extends Response {
  override def statusCode: Int = response.getStatusCode

  override def body: String = response.getResponseBody
}