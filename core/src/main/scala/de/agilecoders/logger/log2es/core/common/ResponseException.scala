package de.agilecoders.logger.log2es.core.common

import java.nio.charset.StandardCharsets

/**
 * special response exception
 *
 * @author Michael Haitz <michael.haitz@agilecoders.de>
 */
case class ResponseException(private val response: com.ning.http.client.Response) extends RuntimeExceptionWithoutStack(
  s"response exception, status: ${response.getStatusCode} ${response.getStatusText}; \n ${response.getResponseBody}"
) {

  /**
   * @return response status code
   */
  def statusCode: Int = response.getStatusCode

  /**
   * @return response body as string
   */
  def responseBody: String = response.getResponseBody(StandardCharsets.UTF_8.displayName())
}
