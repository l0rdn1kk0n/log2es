package de.agilecoders.logback.elasticsearch;

import com.typesafe.config.Config;
import de.agilecoders.logback.elasticsearch.util.Classes;
import io.searchbox.client.JestClient;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Log2es configuration holder
 *
 * @author miha
 */
public class Configuration {

    public static Builder from(Config config) {
        final Builder builder = new Builder();

        builder.conf.retryCount = config.getInt("configuration.retryCount");
        builder.conf.typeName = config.getString("configuration.typeName");
        builder.conf.indexName = config.getString("configuration.indexName");
        builder.conf.discardable = config.getString("configuration.discardable");
        builder.conf.queueSize = config.getInt("configuration.queueSize");
        builder.conf.hosts = config.getStringList("configuration.hosts");
        builder.conf.initializeMapping = config.getBoolean("configuration.initializeMapping");
        builder.conf.transformer = Classes.toTransformerInstance(config.getString("configuration.transformer"));

        List<String> fields = config.getStringList("configuration.fields");
        builder.conf.addLevel = fields.contains("level");
        builder.conf.addTimestamp = fields.contains("timestamp");
        builder.conf.addCaller = fields.contains("caller");
        builder.conf.addMessage = fields.contains("message");
        builder.conf.addLogger = fields.contains("logger");
        builder.conf.addThread = fields.contains("thread");
        builder.conf.addArguments = fields.contains("arguments");
        builder.conf.addDate = fields.contains("date");
        builder.conf.addStacktrace = fields.contains("stacktrace");
        builder.conf.addMdc = fields.contains("mdc");

        return builder;
    }

    private int retryCount;
    private boolean addLevel;
    private boolean addTimestamp;
    private boolean addCaller;
    private boolean addMessage;
    private boolean addLogger;
    private boolean addThread;
    private boolean addArguments;
    private boolean addDate;
    private boolean addStacktrace;
    private boolean addMdc;
    private boolean initializeMapping;
    private String discardable;
    private int queueSize;
    private Collection<String> hosts;
    private String indexName;
    private String typeName;
    private LoggingEventToMapTransformer transformer;

    // TODO miha: design problem
    private JestClient client = null;

    /**
     * Construct.
     */
    private Configuration() {
        // ensure that constructor is private.
    }

    public boolean addCaller() { return addCaller; }

    public boolean addArguments() { return addArguments; }

    public boolean addDate() { return addDate; }

    public boolean addStacktrace() { return addStacktrace; }

    public boolean addMdc() { return addMdc; }

    public boolean addThread() { return addThread; }

    public boolean addMessage() { return addMessage; }

    public boolean addLogger() { return addLogger; }

    public boolean addTimestamp() { return addTimestamp; }

    public boolean addLevel() { return addLevel; }

    public boolean initializeMapping() { return initializeMapping; }

    public int queueSize() { return queueSize; }

    public JestClient client() { return client; }

    public String indexName() { return indexName; }

    public String typeName() { return typeName; }

    public String discardable() { return discardable; }

    public Collection<String> hosts() { return newArrayList(hosts); }

    public LoggingEventToMapTransformer transformer() { return transformer; }

    public int retryCount() { return retryCount; }

    // TODO miha: design problem
    private void close() {
        transformer.withConfiguration(this);
    }

    public static final class Builder {
        private final Configuration conf;

        public Builder() {
            conf = new Configuration();
        }

        public Builder client(JestClient value) {
            conf.client = value;
            return this;
        }

        public Configuration build() {
            conf.close();

            return conf;
        }
    }

}
