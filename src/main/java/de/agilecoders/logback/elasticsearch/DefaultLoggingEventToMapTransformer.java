package de.agilecoders.logback.elasticsearch;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Default implementation of {@link LoggingEventToMapTransformer}
 *
 * @author miha
 */
public class DefaultLoggingEventToMapTransformer implements LoggingEventToMapTransformer {
    private static final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

    private Configuration configuration = null;
    private final Function<StackTraceElement, Map<String, Object>> transformCallerFunction = new Function<StackTraceElement, Map<String, Object>>() {
        @Override
        public Map<String, Object> apply(StackTraceElement stackTraceElement) {
            return transformCaller(stackTraceElement);
        }
    };

    @Override
    public LoggingEventToMapTransformer withConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    @Override
    public Map<String, Object> toMap(final ILoggingEvent event) {
        if (configuration == null) {
            throw new IllegalArgumentException("there's no configuration");
        }

        final Map<String, Object> json = new HashMap<>();

        addBaseValues(json, event);

        if (configuration.addArguments() && event.getArgumentArray() != null && event.getArgumentArray().length > 0) {
            json.put(Keys.arguments, transformArguments(event.getArgumentArray()));
        }

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (configuration.addMdc() && mdc != null && !mdc.isEmpty()) {
            json.put(Keys.mdc, transformMdc(mdc));
        }

        if (configuration.addCaller() && event.hasCallerData()) {
            addCaller(json, event.getCallerData());
        }

        if (configuration.addStacktrace() && event.getThrowableProxy() != null) {
            json.put(Keys.stacktrace, transformThrowable(event.getThrowableProxy()));
        }

        return json;
    }

    /**
     * transforms a mdc property map into a json structure.
     *
     * @param mdc the mdc properties to transform
     * @return the transformed value
     */
    private Object transformMdc(Map<String, String> mdc) {
        return mdc;
    }

    /**
     * adds the caller data to given json structure.
     *
     * @param json       the json structure (map) to add values to.
     * @param callerData the caller data as array of {@link StackTraceElement}
     */
    protected void addCaller(Map<String, Object> json, StackTraceElement[] callerData) {
        json.put(Keys.caller, Iterables.transform(newArrayList(callerData),
                                                  transformCallerFunction));
    }

    protected Map<String, Object> transformCaller(StackTraceElement stackTraceElement) {
        Map<String, Object> map = new HashMap<>();
        map.put(Keys.file, stackTraceElement.getFileName());
        map.put(Keys.clazz, stackTraceElement.getClassName());
        map.put(Keys.method, stackTraceElement.getMethodName());
        map.put(Keys.line, stackTraceElement.getLineNumber());
        map.put(Keys.nativeValue, stackTraceElement.isNativeMethod());

        return map;
    }

    /**
     * adds a set of base values to given json structure.
     *
     * @param json  the json structure (map) to add values to.
     * @param event the logging event.
     */
    protected void addBaseValues(Map<String, Object> json, ILoggingEvent event) {
        if (configuration.addTimestamp()) {
            json.put(Keys.timestamp, event.getTimeStamp());
        }

        if (configuration.addDate()) {
            json.put(Keys.date, transformDate(event.getTimeStamp()));
        }

        if (configuration.addLevel()) {
            json.put(Keys.level, event.getLevel().levelStr);
        }

        if (configuration.addMessage()) {
            json.put(Keys.message, event.getFormattedMessage());
        }

        if (configuration.addLogger()) {
            json.put(Keys.logger, event.getLoggerName());
        }

        if (configuration.addThread()) {
            json.put(Keys.thread, event.getThreadName());
        }

        if (event.getMarker() != null) {
            json.put(Keys.marker, event.getMarker().getName());
        }
    }

    /**
     * transforms a timestamp into its date string format.
     *
     * @param timestamp the timestamp to transform
     * @return string representation of date (defined by timestamp)
     */
    protected String transformDate(long timestamp) {
        return new DateTime(timestamp).toString(formatter);
    }

    protected Object transformArguments(Object[] argumentArray) {
        return argumentArray;
    }

    protected Map<String, Object> transformThrowable(IThrowableProxy throwable) {
        Map<String, Object> map = new HashMap<>();
        map.put(Keys.clazz, throwable.getClassName());
        map.put(Keys.message, throwable.getMessage());
        map.put(Keys.stacktrace, transformStackTraceElement(throwable));

        if (throwable.getCause() != null) {
            map.put(Keys.cause, transformThrowable(throwable.getCause()));
        }

        return map;
    }

    protected String[] transformStackTraceElement(IThrowableProxy throwable) {
        final StackTraceElementProxy[] elementProxies = throwable.getStackTraceElementProxyArray();
        final int totalFrames = elementProxies.length - throwable.getCommonFrames();
        final String[] stackTraceElements = new String[totalFrames];

        for (int i = 0; i < totalFrames; ++i) {
            stackTraceElements[i] = elementProxies[i].getStackTraceElement().toString();
        }

        return stackTraceElements;
    }

    /**
     * defines all available keys of json structure.
     */
    public static final class Keys {
        public static final String line = "line";
        public static final String mdc = "mdc";
        public static final String arguments = "arguments";
        public static final String cause = "cause";
        public static final String caller = "caller";
        public static final String file = "file";
        public static final String clazz = "class";
        public static final String method = "method";
        public static final String nativeValue = "native";
        public static final String stacktrace = "stacktrace";
        public static final String timestamp = "timestamp";
        public static final String marker = "marker";
        public static final String message = "message";
        public static final String level = "level";
        public static final String logger = "logger";
        public static final String thread = "thread";
        public static final String date = "date";
    }
}
