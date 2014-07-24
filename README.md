## log2es

log2es is a [logback](http://logback.qos.ch), [log4j](http://logging.apache.org/log4j/1.2/) and
[log4j2](http://logging.apache.org/log4j/2.x/) appender for [elasticsearch](http://elasticsearch.org) which is based on
 [Akka](http://akka.io) and [Jest](https://github.com/searchbox-io/Jest).
The appender sends asynchronous non blocking bulk HTTP calls to a [elasticsearch](http://elasticsearch) cluster for
each
log event.
CAUTION: there's no stable release version available yet, so please don't use it in your production environment.

### Installation

You've to add the maven dependency first (please add only one of the following, depending on your logger implementation):

log2es is available for scala 2.10 and 2.11.

```xml
<dependency>
    <groupId>de.agilecoders.logback</groupId>
    <artifactId>log2es-logback_2.10</artifactId>
    <version>0.2.1</version>
</dependency>
<dependency>
    <groupId>de.agilecoders.logback</groupId>
    <artifactId>log2es-log4j_2.10</artifactId>
    <version>0.2.1</version>
</dependency>
<dependency>
    <groupId>de.agilecoders.logback</groupId>
    <artifactId>log2es-log4j2_2.10</artifactId>
    <version>0.2.1</version>
</dependency>
```

How to configure logback:

```xml
<appender name="log2es" class="de.agilecoders.logger.log2es.logback.ElasticsearchAppender">
</appender>
```

How to configure log4j:

```
# Set root logger level to DEBUG and its only appender to ES.
log4j.rootLogger=DEBUG, ES

# ES is set to be a ActorBasedElasticSearchLog4jAppender.
log4j.appender.ES=de.agilecoders.logger.log2es.log4j.ElasticsearchAppender
```

How to configure log4j2:

```xml
 <Appenders>
    <ElasticsearchAppender name="log2es">
    </ElasticsearchAppender>
  </Appenders>
```

### Configuration

It is possible to configure all parameters of log2es via `logback.xml`.

```xml
<appender name="log2es" class="de.agilecoders.logger.log2es.logback.ElasticsearchAppender">
    <fields>MESSAGE, THREAD, LEVEL,ARGUMENTS, LOGGER, MARKER,MDC, TIMESTAMP, STACKTRACE,CALLER, SERVICE, HOSTNAME
    </fields>
    <host>http://localhost:9200</host>
    <clientType>http</clientType>
    <gzip>true</gzip>
    <hostName>localhost</hostName>
    <serviceName>log2es-test</serviceName>
    <outgoingBulkSize>5000</outgoingBulkSize>
    <flushQueueTime>1 seconds</flushQueueTime>
</appender>
```

Authors
-------

[![Ohloh profile for Michael Haitz](https://www.ohloh.net/accounts/235496/widgets/account_detailed.gif)](https://www.ohloh.net/accounts/235496?ref=Detailed)


Bug tracker
-----------

Have a bug? Please create an issue here on GitHub!

https://github.com/l0rdn1kk0n/log2es/issues


Versioning
----------

Wicket-Bootstrap will be maintained under the Semantic Versioning guidelines as much as possible.

Releases will be numbered with the follow format:

`<major>.<minor>.<patch>`

And constructed with the following guidelines:

* Breaking backward compatibility bumps the major
* New additions without breaking backward compatibility bumps the minor
* Bug fixes and misc changes bump the patch

For more information on SemVer, please visit http://semver.org/.


Copyright and license
---------------------

Copyright 2012 AgileCoders.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.