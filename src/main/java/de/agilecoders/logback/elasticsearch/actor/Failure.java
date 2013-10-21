package de.agilecoders.logback.elasticsearch.actor;

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
public class Failure {

    private final String errorMessage;
    private final Throwable throwable;
    private final Object message;

    public Failure(final String errorMessage, final Throwable throwable, final Object message) {
        this.errorMessage = errorMessage;
        this.throwable = throwable;
        this.message = message;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Object message() {
        return message;
    }

    public Throwable throwable() {
        return throwable;
    }
}
