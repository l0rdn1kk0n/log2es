package de.agilecoders.logback.elasticsearch.actor;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Holds the {@link ActorSystem} instance.
 *
 * @author miha
 */
public class LogbackActorSystem {
    private static final Logger LOG = LoggerFactory.getLogger(LogbackActorSystem.class);

    /**
     * lazy initializer of {@link ActorSystem} instance
     */
    private static final class Holder {
        private static final ActorSystem instance;
        private static final Config configuration;
        private static final String name = "log2es";

        static {
            Config config = ConfigFactory.load();

            Path customConfig = findCustomConfig();
            if (customConfig != null) {
                config = ConfigFactory.parseFile(customConfig.toFile()).withFallback(config);
            }

            configuration = config.getConfig(name);
            instance = ActorSystem.create(name, configuration);
        }

        private static Path findCustomConfig() {
            Path path = toPath("/" + name + ".conf");

            return path != null ? path : toPath(name + ".conf");
        }

        private static Path toPath(final String pathToConfig) {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(pathToConfig);

            try {
                if (resource != null) {
                    return Paths.get(resource.toURI());
                }
            } catch (RuntimeException | URISyntaxException e) {
                LOG.error("can't load custom configuration", e);
            }

            return null;
        }
    }

    /**
     * @return configuration
     */
    public static Config configuration() {
        return Holder.configuration;
    }

    /**
     * @return the {@link ActorSystem} singleton instance
     */
    public static ActorSystem instance() {
        return Holder.instance;
    }

}
