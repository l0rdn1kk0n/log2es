package de.agilecoders.logger.log2es.core.elasticsearch

/**
 * response abstraction
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
trait Response {

  /**
   * @return response status code
   */
  def statusCode: Int

  /**
   * @return response body as string
   */
  def body: String

}

/**
 * static response object
 *
 * @param statusCode http status code
 * @param body response body as string
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class StaticResponse(statusCode: Int, body: String = "") extends Response

/**
 * http response wrapper
 *
 * @param response the http response
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class HttpResponse(private val response: com.ning.http.client.Response) extends Response {
  override def statusCode: Int = response.getStatusCode

  override def body: String = response.getResponseBody
}