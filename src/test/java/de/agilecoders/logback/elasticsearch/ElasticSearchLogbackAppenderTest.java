package de.agilecoders.logback.elasticsearch;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.core.status.Status;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Marker;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the {@link ElasticSearchLogbackAppender} class
 *
 * @author miha
 */
public class ElasticSearchLogbackAppenderTest {

    private static ElasticSearchLogbackAppender logger;

    @BeforeClass
    public static void setUp() throws Exception {
        logger = new ElasticSearchLogbackAppender();
        logger.setContext(new LoggerContext());
        logger.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        logger.stop();
    }

    @Test
    public void testStartup() {
        assertValidStatus();
    }

    @Test
    public void sendLogMessage() {
        logger.append(new ILoggingEvent() {
            @Override
            public String getThreadName() {
                return "thread-name";
            }

            @Override
            public Level getLevel() {
                return Level.INFO;
            }

            @Override
            public String getMessage() {
                return "message";
            }

            @Override
            public Object[] getArgumentArray() {
                return new Object[0];
            }

            @Override
            public String getFormattedMessage() {
                return getMessage();
            }

            @Override
            public String getLoggerName() {
                return "logger";
            }

            @Override
            public LoggerContextVO getLoggerContextVO() {
                return null;
            }

            @Override
            public IThrowableProxy getThrowableProxy() {
                return null;
            }

            @Override
            public StackTraceElement[] getCallerData() {
                return new StackTraceElement[0];
            }

            @Override
            public boolean hasCallerData() {
                return false;
            }

            @Override
            public Marker getMarker() {
                return null;
            }

            @Override
            public Map<String, String> getMDCPropertyMap() {
                return null;
            }

            @Override
            public Map<String, String> getMdc() {
                return null;
            }

            @Override
            public long getTimeStamp() {
                return 12345678;
            }

            @Override
            public void prepareForDeferredProcessing() {
            }
        });

        assertValidStatus();
    }

    private void assertValidStatus() {
        List<Status> status = logger.getStatusManager().getCopyOfStatusList();

        for (Status s : status) {
            assertThat(s.getLevel(), is(not(Level.WARN_INT)));
            assertThat(s.getLevel(), is(not(Level.ERROR_INT)));
        }
    }

}
