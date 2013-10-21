package de.agilecoders.logback.elasticsearch.actor;

/**
 * TODO miha: document class purpose
 *
 * @author miha
 */
public class LogEvent {

    private final String json;

    public LogEvent(final String json) {
        this.json = json;
    }

    public String json() {
        return json;
    }

    public static LogEvent from(Object eventObject) {
        return new LogEvent(String.valueOf(eventObject));
    }
}
