package de.agilecoders.logback.elasticsearch;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;

/**
 * Defines the interface of all {@link ILoggingEvent} to {@link Map}
 * transformer. This transformer is used to create a json representation
 * of {@link ILoggingEvent} that matches the elasticsearch mapping.
 *
 * @author miha
 */
public interface LoggingEventToMapTransformer {

    /**
     * transforms an {@link ILoggingEvent} to a {@link Map} according to the
     * elasticsearch mapping.
     *
     * @param event the logging event to transform
     * @return logging event as map
     */
    Map<String, Object> toMap(ILoggingEvent event);

    /**
     * the configuration to use for mapping
     *
     * @return this instance for chaining
     */
    LoggingEventToMapTransformer withConfiguration(Configuration configuration);
}
