package de.agilecoders.logback.elasticsearch.actor;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.routing.Broadcast;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.agilecoders.logback.elasticsearch.Configuration;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import scala.concurrent.duration.Duration;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Actor that is responsible for queuing and sending of log messages.
 *
 * @author miha
 */
public class QueuedWorkerActor extends UntypedActor {

    private static final int INTERVAL_TIME_IN_MSEC = 1000;
    private static Cancellable newScheduler(UntypedActorContext context, ActorRef self) {
        int startDelay = 100 + new Random().nextInt(INTERVAL_TIME_IN_MSEC);

        return context.system().scheduler().schedule(
                Duration.create(startDelay, TimeUnit.MILLISECONDS),
                Duration.create(INTERVAL_TIME_IN_MSEC, TimeUnit.MILLISECONDS),
                self, new FlushQueue(), context.dispatcher(), null);
    }

    private final Cancellable scheduler = newScheduler(getContext(), getSelf());
    private final Configuration configuration;

    private int count = 0;
    private Bulk.Builder builder;

    /**
     * Construct.
     *
     * @param configuration the configuration to use
     */
    public QueuedWorkerActor(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return new bulk action builder instance
     */
    private Bulk.Builder newBuilder() {
        return new Bulk.Builder()
                .defaultIndex(configuration.indexName())
                .defaultType(configuration.typeName());
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        reset();
    }

    private void reset() {
        this.builder = newBuilder();
        this.count = 0;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof LogEvent) {
            queue(((LogEvent) message).json());

            count++;
            flushIfNecessary();
        } else if (message instanceof ILoggingEvent) {
            queue((ILoggingEvent) message);

            count++;
            flushIfNecessary();
        } else if (message instanceof FlushQueue || message instanceof PoisonPill) {
            flush();
        } else if (message instanceof Broadcast) {
            Broadcast broadcast = (Broadcast) message;

            if (broadcast.message() instanceof FlushQueue ||
                broadcast.message() instanceof PoisonPill) {
                flush();
            }
        } else {
            unhandled(message);
        }
    }

    /**
     * flush all log messages if queue size was reached
     */
    private void flushIfNecessary() {
        if (count > configuration.queueSize()) {
            flush();
        }
    }

    /**
     * flush all log messages if queue contains at least one element
     */
    private void flush() {
        if (count > 0) {
            send(builder.build());
        }
    }

    /**
     * sends all queued log messages to elasticsearch cluster.
     *
     * @param bulk the bulk operation that holds all log messages.
     */
    private void send(Bulk bulk) {
        try {
            configuration.client().execute(bulk);

            reset();
        } catch (Exception e) {
            getSender().tell(new Failure(e.getMessage(), e, null), getSelf());
        }
    }

    /**
     * adds a new log message to the queue
     *
     * @param json the json data to queue
     */
    private void queue(String json) {
        builder.addAction(newIndex(json));
    }

    /**
     * adds a new log message to the queue
     *
     * @param event the event to add to queue
     */
    private void queue(ILoggingEvent event) {
        builder.addAction(newIndex(configuration.transformer().toMap(event)));
    }

    /**
     * creates a new index action
     *
     * @param data the data to index
     * @return new index action
     */
    private Index newIndex(final Object data) {
        return new Index.Builder(data)
                .index(configuration.indexName())
                .type(configuration.typeName())
                .build();
    }

    @Override
    public void postStop() {
        scheduler.cancel();
    }

}
