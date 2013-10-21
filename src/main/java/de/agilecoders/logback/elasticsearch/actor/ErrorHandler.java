package de.agilecoders.logback.elasticsearch.actor;

import akka.actor.DeadLetter;
import akka.actor.UntypedActor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import de.agilecoders.logback.elasticsearch.Configuration;
import de.agilecoders.logback.elasticsearch.ElasticSearchLogbackAppender;
import org.slf4j.Marker;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * handles all actor errors
 *
 * @author miha
 */
public class ErrorHandler extends UntypedActor {
    private final ElasticSearchLogbackAppender appender;
    private final Configuration configuration;

    /**
     * Construct.
     *
     * @param appender      the logback to elasticsearch appender
     * @param configuration the base configuration
     */
    ErrorHandler(ElasticSearchLogbackAppender appender, Configuration configuration) {
        this.appender = appender;
        this.configuration = configuration;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Failure) {
            handleFailure((Failure) message);
        } else if (message instanceof DeadLetter) {
            handleDeadLetter((DeadLetter) message);
        } else {
            unhandled(message);
        }
    }

    private void handleDeadLetter(DeadLetter message) {
        if (message.message() instanceof ILoggingEvent) {
            retryOrDiscard(message.message());
        } else {
            appender.addError("can't transfer message: " + message);
        }
    }

    private void handleFailure(Failure failure) {
        if (failure.message() != null) {
            retryOrDiscard(failure.message());
        } else {
            appender.addError(failure.errorMessage(), failure.throwable());
        }
    }

    private void retryOrDiscard(Object message) {
        if (message instanceof RetryLoggingEvent) {
            RetryLoggingEvent event = (RetryLoggingEvent) message;

            if (event.counter() <= configuration.retryCount()) {
                appender.doAppend(RetryLoggingEvent.retry(event));
            } else {
                appender.addError("dropped non-discardable event: " + event.toString());
            }
        } else if (message instanceof ILoggingEvent) {
            ILoggingEvent event = (ILoggingEvent) message;

            if (appender.isDiscardable(event)) {
                appender.addWarn("dropped discardable event: " + event.toString());
            } else {
                appender.doAppend(RetryLoggingEvent.retry(event));
            }
        }
    }

    private static final class RetryLoggingEvent implements ILoggingEvent {
        private final AtomicInteger counter;
        private final ILoggingEvent backingEvent;

        private RetryLoggingEvent(ILoggingEvent backingEvent) {
            this.backingEvent = backingEvent;
            this.counter = new AtomicInteger(1);
        }

        public static ILoggingEvent retry(ILoggingEvent event) {
            if (event instanceof RetryLoggingEvent) {
                ((RetryLoggingEvent) event).counter.incrementAndGet();
                return event;
            }

            return new RetryLoggingEvent(event);
        }

        public int counter() {
            return counter.get();
        }

        @Override
        public String getThreadName() {
            return backingEvent.getThreadName();
        }

        @Override
        public Level getLevel() {
            return backingEvent.getLevel();
        }

        @Override
        public String getMessage() {
            return backingEvent.getMessage();
        }

        @Override
        public Object[] getArgumentArray() {
            return backingEvent.getArgumentArray();
        }

        @Override
        public String getFormattedMessage() {
            return backingEvent.getFormattedMessage();
        }

        @Override
        public String getLoggerName() {
            return backingEvent.getLoggerName();
        }

        @Override
        public LoggerContextVO getLoggerContextVO() {
            return backingEvent.getLoggerContextVO();
        }

        @Override
        public IThrowableProxy getThrowableProxy() {
            return backingEvent.getThrowableProxy();
        }

        @Override
        public StackTraceElement[] getCallerData() {
            return backingEvent.getCallerData();
        }

        @Override
        public boolean hasCallerData() {
            return backingEvent.hasCallerData();
        }

        @Override
        public Marker getMarker() {
            return backingEvent.getMarker();
        }

        @Override
        public Map<String, String> getMDCPropertyMap() {
            return backingEvent.getMDCPropertyMap();
        }

        @Override
        public Map<String, String> getMdc() {
            return backingEvent.getMdc();
        }

        @Override
        public long getTimeStamp() {
            return backingEvent.getTimeStamp();
        }

        @Override
        public void prepareForDeferredProcessing() {
            backingEvent.prepareForDeferredProcessing();
        }

        @Override
        public String toString() {
            return backingEvent.toString();
        }
    }

}