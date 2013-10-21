package de.agilecoders.logback.elasticsearch.actor;

import akka.actor.Props;
import akka.japi.Creator;
import de.agilecoders.logback.elasticsearch.Configuration;

/**
 * The {@link WorkerActorCreator} is responsible of creating new worker
 * actor instances.
 *
 * @author miha
 */
public class WorkerActorCreator implements Creator<QueuedWorkerActor> {

    /**
     * creates a new actor props object.
     *
     * @param configuration the configuration to use
     * @return new {@link Props} instance
     */
    public static Props createActor(Configuration configuration) {
        return Props.create(new WorkerActorCreator(configuration));
    }

    private final Configuration configuration;

    /**
     * Construct.
     *
     * @param configuration the configuration to use
     */
    public WorkerActorCreator(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public QueuedWorkerActor create() throws Exception {
        return new QueuedWorkerActor(configuration);
    }
}
