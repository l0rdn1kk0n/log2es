## log2es

log2es is a [logback](http://logback.qos.ch) appender for [elasticsearch](http://elasticsearch.org) which is based on
 [Akka](http://akka.io) and [Jest](https://github.com/searchbox-io/Jest).
The appender sends asynchronous non blocking bulk HTTP calls to a [elasticsearch](http://elasticsearch) cluster for
each
log event.
CAUTION: there's no stable release version available yet, so please don't use it in your production environment.

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
        # all available elasticsearch hosts (mandatory)
        hosts = ["http://localhost:9200"]

        # defines how many retries will be tried till message will be dropped (optional)
        retryCount = 3

        # defines the internal queue size for elasticsearch bulk operations to reduce
        # number of http calls (optional)
        queueSize = 100

        # the elasticsearch mapping type name (optional)
        typeName = logline

        # the elasticsearch mapping index name (optional)
        indexName = log

        # whether to initialize mapping or not (optional)
        initializeMapping = true

        # Events of this level are deemed to be discardable. (optional)
        # e.g. if set to INFO then TRACE, DEBUG and INFO are discardable
        discardable = INFO

        # defines all fields that will be sent to elasticsearch (optional)
        fields = ["date", "level", "logger", "thread", "message", "stacktrace"]

        # transformer class to transform logging events to json   (optional)
        transformer = "de.agilecoders.logback.elasticsearch.DefaultLoggingEventToMapTransformer"
    }

}
```
