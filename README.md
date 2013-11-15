## log2es

log2es is a [logback](http://logback.qos.ch), [log4j](http://logging.apache.org/log4j/1.2/) and
[log4j2](http://logging.apache.org/log4j/2.x/) appender for [elasticsearch](http://elasticsearch.org) which is based on
 [Akka](http://akka.io) and [Jest](https://github.com/searchbox-io/Jest).
The appender sends asynchronous non blocking bulk HTTP calls to a [elasticsearch](http://elasticsearch) cluster for
each
log event.
CAUTION: there's no stable release version available yet, so please don't use it in your production environment.

### Installation

You've to add the maven dependency first (please add only one of the following):

```xml
<dependency>
    <groupId>de.agilecoders.logback</groupId>
    <artifactId>log2es-logback</artifactId>
    <version>0.2.1</version>
</dependency>
<dependency>
    <groupId>de.agilecoders.logback</groupId>
    <artifactId>log2es-log4j</artifactId>
    <version>0.2.1</version>
</dependency>
<dependency>
    <groupId>de.agilecoders.logback</groupId>
    <artifactId>log2es-log4j2</artifactId>
    <version>0.2.1</version>
</dependency>
```

How to configure logback:

```xml
<appender name="log2es" class="de.agilecoders.elasticsearch.logger.logback.ActorBasedElasticSearchLogbackAppender">
</appender>
```

How to configure log4j:

```
# Set root logger level to DEBUG and its only appender to ES.
log4j.rootLogger=DEBUG, ES

# ES is set to be a ActorBasedElasticSearchLog4jAppender.
log4j.appender.ES=de.agilecoders.elasticsearch.logger.log4j.ActorBasedElasticSearchLog4jAppender
```

How to configure log4j2:

```xml
 <Appenders>
    <!-- Async Loggers will auto-flush in batches, so switch off immediateFlush. -->
    <ActorBasedElasticSearchLog4j2Appender name="log2es">
    </ActorBasedElasticSearchLog4j2Appender>
  </Appenders>
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
