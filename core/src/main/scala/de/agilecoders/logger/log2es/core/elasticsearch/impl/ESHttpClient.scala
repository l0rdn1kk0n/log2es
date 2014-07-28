package de.agilecoders.logger.log2es.core.elasticsearch.impl

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.common.ResponseException
import de.agilecoders.logger.log2es.core.elasticsearch.{ESClient, EventsBodyWriter, HttpResponse, Response}
import org.json4s._

import scala.concurrent.Promise
import scala.util.{Failure, Success}

/**
 * elasticsearch client that uses the http API
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class ESHttpClient(conf: Configuration) extends ESClient {

  import dispatch.Defaults._
  import dispatch._

  // todo: allow custom configuration of http client
  private lazy val client = Http().configure(builder => {
    builder
      .setAllowPoolingConnection(true)
      .setCompressionEnabled(conf.gzip)
      // NOT SUPPORTED by netty, .setRequestCompressionLevel(6)
      .setMaximumConnectionsPerHost(3)
      .setMaxRequestRetry(2)
      .setFollowRedirects(false)
  })

  private lazy val hostname = host("localhost", 9200)

  private lazy val indexRequestBuilder = (hostname / conf.indexName).POST

  override def send(indexName: String, typeName: String, events: Seq[String]) = {
    val content = EventsBodyWriter(events, conf.gzip)
    val request = indexRequestBuilder / typeName / "_bulk"

    execute(request setBody content)
  }

  override def updateIndex(indexName: String, typeName: String, configuration: String) = {
    val f = Promise[Response]()

    val update = (f: Promise[Response]) => {
      import org.json4s.jackson.JsonMethods._
      val json: JValue = parse(configuration)

      f completeWith execute((hostname / indexName / typeName / "_mapping").PUT.setBody(compact(json.\("mappings"))))
    }

    val r = (hostname / indexName).PUT.setBody(configuration)

    if (conf.gzip) {
      r.setHeader("Content-Encoding", "gzip")
    }

    execute(r).onComplete({
      case Success(response) if response.statusCode == 200 =>
        f.complete(Success(response))
      case Success(response) if response.statusCode == 400 =>
        update(f)
      case Failure(e: ResponseException) if e.statusCode == 400 =>
        update(f)
      case Failure(e) =>
        f.failure(e)
    })

    f.future
  }

  override def status() = {
    execute(hostname / "_cluster" / "health" GET) map (_.body)
  }

  private def execute(req: Req): scala.concurrent.Future[Response] = {
    val f = Promise[Response]()

    client.apply(req
      .setContentType("application/json", "UTF-8")
      .setHeader("User-Agent", conf.userAgent)).onComplete({
      case Success(response) if response.getStatusCode < 400 =>
        f.complete(Success(HttpResponse(response)))
      case Success(response) if response.getStatusCode >= 400 =>
        f.failure(ResponseException(response))
      case Failure(e) =>
        f.failure(e)
    })

    f.future
  }

  override def shutdown() = {
    client.shutdown()
  }

  override def start() = {}
}

