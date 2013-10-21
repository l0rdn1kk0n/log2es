package de.agilecoders.logback.elasticsearch.actor;

import akka.actor.Props;
import akka.japi.Creator;
import de.agilecoders.logback.elasticsearch.Configuration;
import de.agilecoders.logback.elasticsearch.ElasticSearchLogbackAppender;

/**
 * The {@link de.agilecoders.logback.elasticsearch.actor.ErrorHandlerCreator} is responsible of creating new worker
 * actor instances.
 *
 * @author miha
 */
public class ErrorHandlerCreator implements Creator<ErrorHandler> {

    /**
     * creates a new actor props object.
     *
     * @param configuration the configuration to use
     * @return new {@link akka.actor.Props} instance
     */
    public static Props createActor(ElasticSearchLogbackAppender contextAwareBase, Configuration configuration) {
        return Props.create(new ErrorHandlerCreator(contextAwareBase, configuration));
    }

    private final ElasticSearchLogbackAppender contextAwareBase;
    private final Configuration configuration;

    /**
     * Construct.
     *
     * @param contextAwareBase the logback context
     * @param configuration the configuration to use
     */
    public ErrorHandlerCreator(ElasticSearchLogbackAppender contextAwareBase, Configuration configuration) {
        this.contextAwareBase = contextAwareBase;
        this.configuration = configuration;
    }

    @Override
    public ErrorHandler create() throws Exception {
        return new ErrorHandler(contextAwareBase, configuration);
    }
}
