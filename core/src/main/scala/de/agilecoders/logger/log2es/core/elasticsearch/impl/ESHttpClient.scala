package de.agilecoders.logger.log2es.core.elasticsearch.impl

import de.agilecoders.logger.log2es.core.Configuration
import de.agilecoders.logger.log2es.core.common.RuntimeExceptionWithoutStack
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

  private lazy val hostname = host("localhost", 9200)

  private lazy val indexRequestBuilder = (hostname / conf.indexName).POST

  override def send(indexName: String, typeName: String, events: Seq[String]) = {
    val content = EventsBodyWriter(events, conf.gzip)
    val request = indexRequestBuilder / typeName / "_bulk"

    execute(request setBody content)
  }

  override def updateIndex(indexName: String, typeName: String, configuration: String) = {
    val f = Promise[Response]()

    execute((hostname / indexName).PUT.setBody(configuration)).onComplete({
      case Success(response) if response.statusCode == 200 =>
        f.complete(Success(response))
      case Success(response) if response.statusCode == 400 =>
        import org.json4s.jackson.JsonMethods._
        val json: JValue = parse(configuration)

        f completeWith execute((hostname / indexName / typeName / "_mapping").PUT.setBody(compact(json.\("mappings"))))
      case Success(response) =>
        f.failure(new RuntimeExceptionWithoutStack("can't update settings: " + response.statusCode))
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
