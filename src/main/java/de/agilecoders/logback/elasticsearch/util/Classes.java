package de.agilecoders.logback.elasticsearch.util;

import de.agilecoders.logback.elasticsearch.DefaultLoggingEventToMapTransformer;
import de.agilecoders.logback.elasticsearch.LoggingEventToMapTransformer;

/**
 * helper class to transform class names into a instance
 *
 * @author miha
 */
public final class Classes {
    /**
     * creates a new instance of given class name.
     *
     * @param transformerClass the class name to use
     * @return new {@link de.agilecoders.logback.elasticsearch.LoggingEventToMapTransformer} instance
     */
    public static LoggingEventToMapTransformer toTransformerInstance(final String transformerClass) {
        try {
            return (LoggingEventToMapTransformer) Class.forName(transformerClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | RuntimeException e) {
            return new DefaultLoggingEventToMapTransformer();
        }
    }
}
