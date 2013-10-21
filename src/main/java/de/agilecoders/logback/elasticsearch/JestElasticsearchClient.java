package de.agilecoders.logback.elasticsearch;

import com.google.common.base.Preconditions;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.ClientConfig;
import io.searchbox.indices.mapping.PutMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * elasticsearch client wrapper.
 *
 * @author miha
 */
public class JestElasticsearchClient implements ElasticsearchClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(JestElasticsearchClient.class);

    private final Configuration configuration;
    private final JestClient client;

    public JestElasticsearchClient(Configuration configuration) {
        this.configuration = configuration;
        this.client = newFactory(configuration).getObject();

        if (configuration.initializeMapping()) {
            addMapping();
        }
    }

    public JestClient client() {
        return client;
    }

    protected JestClientFactory newFactory(Configuration configuration) {
        JestClientFactory factory = new JestClientFactory();
        factory.setClientConfig(newConfig(configuration.hosts()));

        return factory;
    }

    private JestElasticsearchClient addMapping() {
        PutMapping putMapping = new PutMapping.Builder(
                configuration.indexName(),
                configuration.typeName(),
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
        ).build();

        try {
            client.execute(putMapping);
        } catch (Exception e) {
            LOGGER.error("can't add mapping", e);
        }

        return this;
    }

    protected ClientConfig newConfig(final Collection<String> hosts) {
        Preconditions.checkArgument(hosts != null && !hosts.isEmpty());

        return new ClientConfig.Builder(hosts)
                .multiThreaded(true)
                .discoveryEnabled(true)
                .discoveryFrequency(3, TimeUnit.SECONDS)
                .defaultMaxTotalConnectionPerRoute(10)
                .maxTotalConnection(20)
                .build();
    }

    /**
     * shutdown elasticsearch client
     */
    public void shutdown() {
        client.shutdownClient();
    }

}