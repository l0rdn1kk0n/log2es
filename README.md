
## log2es

log2es is a [logback](http://logback.qos.ch) appender for [elasticsearch](http://elasticsearch.org).
The appender sends asynchronous non blocking bulk HTTP calls to a [elasticsearch](http://elasticsearch) cluster for
each
log event.

### Installation

You've to add the maven dependency first:

```xml
<dependency>
    <groupId>de.agilecoders.logback</groupId>
    <artifactId>log2es</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Now you can add the appender to your logback configuration:

```xml
    <appender name="log2es" class="de.agilecoders.logback.elasticsearch.ElasticSearchLogbackAppender">
    </appender>
```

### Configuration

It is possible to configure all parameters of log2es via `src/main/resources/log2es.conf`.

```
log2es {
    configuration {
        # all available elasticsearch hosts
        hosts = ["http://localhost:9200"]

        # defines how many retries will be tried till message will be dropped
        retryCount = 3

        # defines the internal queue size for elasticsearch bulk operations to reduce
        # number of http calls
        queueSize = 100

        # the elasticsearch mapping type name
        typeName = logline

        # the elasticsearch mapping index name
        indexName = log

        # whether to initialize mapping or not
        initializeMapping = true

        # Events of this level are deemed to be discardable.
        # e.g. if set to INFO then TRACE, DEBUG and INFO are discardable
        discardable = INFO

        # defines all fields that will be sent to elasticsearch
        fields = ["date", "level", "logger", "thread", "message", "stacktrace"]

        # transformer class to transform logging events to json
        transformer = "de.agilecoders.logback.elasticsearch.DefaultLoggingEventToMapTransformer"
    }
}
```