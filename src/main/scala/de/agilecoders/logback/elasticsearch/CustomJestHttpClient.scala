package de.agilecoders.logback.elasticsearch

import io.searchbox.client.http.JestHttpClient
import de.agilecoders.logback.elasticsearch.actor.Configuration

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
case class CustomJestHttpClient(configuration: Configuration, private val c:JestHttpClient) extends JestHttpClient {
    private[this] var _isAlive = true

    setAsyncClient(c.getAsyncClient)
    setEntityEncoding(c.getEntityEncoding)
    setGson(c.getGson)
    setHttpClient(c.getHttpClient)
    setNodeChecker(node)

    override def shutdownClient() {
        _isAlive = false
        super.shutdownClient()

        getHttpClient.getConnectionManager.shutdown()
    }

    def isAlive = _isAlive

}
