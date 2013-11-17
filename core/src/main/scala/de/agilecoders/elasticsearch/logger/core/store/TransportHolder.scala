package de.agilecoders.elasticsearch.logger.core.store

import de.agilecoders.elasticsearch.logger.core.Lifecycle
import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.{ImmutableSettings, Settings}
import org.elasticsearch.common.transport.InetSocketTransportAddress

/**
 * Creates and holds a transport client instance
 */
case class TransportHolder(configuration: Configuration) extends Lifecycle[TransportClient] {

    private[this] lazy val client: TransportClient = newTransportClient()

    /**
     * @return new transport client instance
     */
    protected def newTransportClient(): TransportClient = addHosts(new TransportClient(newSettings()))

    /**
     * @return new immutable set of settings that is used to configure a transport client
     */
    protected def newSettings(): Settings = ImmutableSettings.settingsBuilder()
      .put("node.data", false)
      .put("node.client", true)
      .put("client.transport.sniff", configuration.sniffHostnames)
      .put("client.transport.ignore_cluster_name", configuration.clusterName == "")
      .put("cluster.name", configuration.clusterName)
      .build()

    /**
     * adds all configured hosts to given transport client
     *
     * @param client the transport client to add hosts to
     * @return configured transport client
     */
    protected def addHosts(client: TransportClient): TransportClient = {
        configuration.hosts.map(_.split(":")).foreach(host => {
            client.addTransportAddress(new InetSocketTransportAddress(host(0), Integer.parseInt(host(1))))
        })

        client
    }

    /**
     * @return the transport client instance
     */
    def get: TransportClient = client

    /**
     * stops the transport client
     */
    def stop() = get.close()

    /**
     * start is called to startup the class that implements this interface
     *
     * @return the transport client instance
     */
    def start() = get
}
