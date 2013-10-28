package de.agilecoders.logback.elasticsearch

import de.agilecoders.logback.elasticsearch.actor.Configuration
import io.searchbox.client.config.ClientConfig
import io.searchbox.client.http.JestHttpClient
import io.searchbox.client.{JestClientFactory, JestClient}
import io.searchbox.indices.mapping.PutMapping
import java.util.concurrent.TimeUnit
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.apache.http.nio.client.HttpAsyncClient
import org.slf4j.LoggerFactory
import scala.collection.convert.WrapAsJava._

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
object JestElasticsearchClientFactory {
    private[this] val lock = new Object

    private[this] var _instance: Option[CustomJestHttpClient] = None

    def newInstance(configuration: Configuration): CustomJestHttpClient = _instance match {
        case Some(client: CustomJestHttpClient) if client.isAlive => client
        case _ => lock.synchronized {
                                        val instance = JestElasticsearchClientFactory(configuration).create()
                                        _instance = Some(instance)
                                        instance
                                    }
    }

    def shutdown() = _instance match {
        case Some(client: JestClient) => client.shutdownClient()
        case _ => // nothing to do...
    }
}

case class JestElasticsearchClientFactory(configuration: Configuration) {
    private[this] val logger = LoggerFactory.getLogger(classOf[JestElasticsearchClientFactory])
    private[this] var _instance: Option[CustomJestHttpClient] = None

    def newAsyncClient(): HttpAsyncClient = {
        new DefaultHttpAsyncClient(newAsyncConfig())
    }

    private[this] def configure(client: CustomJestHttpClient): JestClient = {
        client.getAsyncClient.shutdown()
        client.setAsyncClient(newAsyncClient())

        addMapping(client)
        client
    }

    def store(client: CustomJestHttpClient): CustomJestHttpClient = {
        _instance = Some(client)
        client
    }

    def create(): CustomJestHttpClient = _instance match {
        case Some(client) => client
        case _ => {
            val client = newFactory().getObject.asInstanceOf[JestHttpClient]
            val instance = CustomJestHttpClient(configuration, client)

            store(configure(instance))
        }
    }

    /**
     * creates a new jest client config
     *
     * @param hosts all available elasticsearch hosts
     * @return new config
     */
    private[this] def newConfig(hosts: Iterable[String]): ClientConfig = hosts match {
        case h: Iterable[String] => new ClientConfig.Builder(hosts)
          .multiThreaded(configuration.multiThreaded)
          .discoveryEnabled(configuration.discoveryEnabled)
          .discoveryFrequency(configuration.discoveryFrequency, TimeUnit.SECONDS)
          .defaultMaxTotalConnectionPerRoute(configuration.defaultMaxTotalConnectionPerRoute)
          .maxTotalConnection(configuration.maxTotalConnection)
          .build()
        case _ => throw new IllegalArgumentException("there's no host list")
    }

    private[this] def newAsyncConfig(): IOReactorConfig = {
        val config = new IOReactorConfig()
        config.setConnectTimeout(500)
        config.setSoTimeout(3000)
        config.setShutdownGracePeriod(30000)

        config
    }

    private[this] def newFactory(): JestClientFactory = {
        val factory = new JestClientFactory()
        factory.setClientConfig(newConfig(configuration.hosts))

        factory
    }

    private[this] def addMapping(client: JestClient) = {
        val putMapping = new PutMapping.Builder(
            configuration.indexName,
            configuration.typeName,
            "{ \"document\" : { \"properties\" : { "
            + "\"message\" : {\"type\" : \"string\", \"store\" : \"yes\"}, "
            + "\"level\" : {\"type\" : \"string\", \"store\" : \"yes\"},"
            + "\"logger\" : {\"type\" : \"string\"},"
            + "\"timestamp\" : {\"type\" : \"long\", \"store\" : \"yes\"},"
            + "\"date\" : {\"type\" : \"date\", \"format\":\"date_time\" \"store\" : \"yes\"},"
            + "\"thread\" : {\"type\" : \"string\"},"
            + "\"mdc\" : {\"type\" : \"string\", \"store\" : \"yes\"},"
            + "\"arguments\" : {\"type\" : \"object\", \"enabled\": false},"
            + "\"caller\" : {\"type\" : \"object\", \"enabled\": false},"
            + "\"stacktrace\" : {\"type\" : \"object\", \"enabled\": false},"
            + "\"marker\" : {\"type\" : \"string\"}"
            + "} } }"
        ).build()

        try {
            client.execute(putMapping)
        } catch {
            case e: Exception => logger.error("can't add mapping", e)
        }
    }

    /**
     * shutdown elasticsearch client
     */
    def shutdown() = _instance match {
        case Some(client) => client.shutdownClient()
        case _ => // already shut down / never created before
    }

}
