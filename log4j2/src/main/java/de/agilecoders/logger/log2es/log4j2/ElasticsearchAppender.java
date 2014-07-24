package de.agilecoders.logger.log2es.log4j2;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

/**
 * Created by miha on 24.07.14.
 */
@Plugin(name = "Elasticsearch", category = "Core", elementType = "appender", printObject = true)
public class ElasticsearchAppender extends AppenderBase {

    @PluginFactory
    public static ElasticsearchAppender createAppender(@PluginAttribute("name")  String name,
                       @PluginAttribute("typeNameUpdateInterval") String typeNameUpdateInterval,
                       @PluginAttribute("actorSystemName") String actorSystemName,
                       @PluginAttribute("serviceName")  String serviceName,
                       @PluginAttribute("hostName") String hostName,
                       @PluginAttribute("indexName")  String indexName,
                       @PluginAttribute("typeName")  String typeName,
                       @PluginAttribute("host")  String host,
                       @PluginAttribute("clientType")  String clientType,
                       @PluginAttribute("clusterName")  String clusterName,
                       @PluginAttribute("userAgent")  String userAgent,
                       @PluginAttribute("ttl")  String ttl,
                       @PluginAttribute("updateMapping")  String updateMapping,
                       @PluginAttribute("gzip")  String gzip,
                       @PluginAttribute("esConfigurationFile")  String esConfigurationFile,
                       @PluginAttribute("fields")  String fields,
                       @PluginAttribute("flushQueueTime")  String flushQueueTime,
                       @PluginAttribute("defaultTimeout") String defaultTimeout,
                       @PluginAttribute("incomingBufferSize") String incomingBufferSize,
                       @PluginAttribute("outgoingBulkSize") String outgoingBulkSize,
                       @PluginElement("Layout") Layout<? extends Serializable> layout,
                       @PluginElement("Filters") Filter filter) {

        ElasticsearchAppender appender = new ElasticsearchAppender(name, filter, layout);

        if (typeNameUpdateInterval != null) {
            appender.setTypeNameUpdateInterval(typeNameUpdateInterval);
        }
        if (clusterName != null) {
            appender.setClusterName(clusterName);
        }
        if (fields != null) {
            appender.setFields(fields);
        }
        if (flushQueueTime != null) {
            appender.setFlushQueueTime(flushQueueTime);
        }
        if (defaultTimeout != null) {
            appender.setDefaultTimeout(defaultTimeout);
        }
        if (incomingBufferSize != null) {
            appender.setIncomingBufferSize(Integer.parseInt(incomingBufferSize));
        }
        if (outgoingBulkSize != null) {
            appender.setOutgoingBulkSize(Integer.parseInt(outgoingBulkSize));
        }
        if (userAgent != null) {
            appender.setUserAgent(userAgent);
        }
        if (ttl != null) {
            appender.setTtl(ttl);
        }
        if (updateMapping != null) {
            appender.setUpdateMapping(Boolean.getBoolean(updateMapping));
        }
        if (gzip != null) {
            appender.setGzip(Boolean.getBoolean(gzip));
        }
        if (esConfigurationFile != null) {
            appender.setEsConfigurationFile(esConfigurationFile);
        }
        if (actorSystemName != null) {
            appender.setActorSystemName(actorSystemName);
        }
        if (serviceName != null) {
            appender.setServiceName(serviceName);
        }
        if (hostName != null) {
            appender.setHostName(hostName);
        }
        if (indexName != null) {
            appender.setIndexName(indexName);
        }
        if (typeName != null) {
            appender.setTypeName(typeName);
        }
        if (host != null) {
            appender.setHostName(host);
        }
        if (clientType != null) {
            appender.setClientType(clientType);
        }
        if (serviceName != null) {
            appender.setServiceName(serviceName);
        }
        if (serviceName != null) {
            appender.setServiceName(serviceName);
        }
        if (serviceName != null) {
            appender.setServiceName(serviceName);
        }

        return appender;
    }

    private ElasticsearchAppender(String _name, Filter filter, Layout<? extends Serializable> layout) {
        super(_name, filter, layout);
    }
}
