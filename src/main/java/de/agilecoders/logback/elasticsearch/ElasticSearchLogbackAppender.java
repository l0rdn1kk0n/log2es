package de.agilecoders.logback.elasticsearch;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.PoisonPill;
import akka.routing.Broadcast;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import de.agilecoders.logback.elasticsearch.actor.ErrorHandlerCreator;
import de.agilecoders.logback.elasticsearch.actor.FlushQueue;
import de.agilecoders.logback.elasticsearch.actor.LogbackActorSystem;
import de.agilecoders.logback.elasticsearch.actor.WorkerActorCreator;

/**
 * Special {@link ch.qos.logback.core.Appender} that is able to send all
 * log messages to an elasticsearch cluster. All log events will be handled
 * asynchronous.
 * <p/>
 * In order to optimize performance this appender deems events of level TRACE,
 * DEBUG and INFO as discardable
 *
 * @author miha
 */
public class ElasticSearchLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private ActorSystem system;
    private ActorRef actor;
    private ActorRef errorHandler;
    private Configuration configuration;

    /**
     * @return actor reference to clustered worker
     */
    private ActorRef actor() {
        return actor;
    }

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event the logging event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    public boolean isDiscardable(ILoggingEvent event) {
        return event.getLevel().toInt() <= Level.valueOf(configuration.discardable()).levelInt;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (isStarted()) {
            actor().tell(eventObject, errorHandler);
        }
    }

    @Override
    public void start() {
        if (!isStarted()) {
            super.start();

            this.system = LogbackActorSystem.instance();

            Configuration.Builder configurationBuilder = Configuration.from(LogbackActorSystem.configuration());
            configuration = configurationBuilder.build();
            configuration = configurationBuilder
                    .client(new JestElasticsearchClient(configuration).client())
                    .build();

            this.errorHandler = system.actorOf(ErrorHandlerCreator.createActor(this, configuration), "error-handler");
            system.eventStream().subscribe(errorHandler, DeadLetter.class);

            this.actor = system.actorOf(WorkerActorCreator.createActor(configuration), "queued-worker");
        }
    }

    @Override
    public void stop() {
        if (isStarted()) {
            super.stop();

            try {
                actor().tell(new Broadcast(new FlushQueue()), errorHandler);
            } catch (Exception e) {
                addWarn("can't send flush queues to actors", e);
            }

            try {
                actor().tell(new Broadcast(PoisonPill.getInstance()), errorHandler);
            } catch (Exception e) {
                addWarn("can't send graceful stop to actors", e);
            }

            try {
                system.stop(actor());
                system.shutdown();
            } catch (RuntimeException e) {
                addWarn(e.getMessage(), e);
            }

            try {
                if (configuration != null) {
                    configuration.client().shutdownClient();
                }
            } catch (RuntimeException e) {
                addWarn(e.getMessage(), e);
            }
        }
    }

}
